import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class client
{
	private JPanel mainPanel;
	private JTextField message_text;
	private JButton send_button;
	private JButton connect_button;
	private JTextField username_text;
	private JTextField hexip_text;
	private JTextPane chat_text;
	private JButton disconnect_button;
	private JTextPane user_list;

	private static final int CONNECT = 0;
	private static final int NORMAL_MESSAGE = 1;
	private static final int PRIVATE_MESSAGE = 2;
	private static final int DISCONNECT = 3;
	private static final int STILL_ALIVE = 4;
	private static final int USER_LIST = 5;
	private static final int ERROR = 6;
	private Document doc;
	private Document users_doc;
	private Socket socket;
	private Boolean keepListening = true;
	private String username;

	private client()
	{
		connect_button.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				connect();
			}
		});
		send_button.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				sendMessage();
			}
		});
		disconnect_button.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				disconnect();
			}
		});
	}

	public static void main(String[] args)
	{
		JFrame frame = new JFrame("App");
		frame.setContentPane(new client().mainPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private boolean legalUsername(String username)
	{
		if (username.length() == 0 || username.contains(" "))
			return false;
		Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(username);
		if (m.find())
			return false;
		return true;
	}

	private String hexToIP(String hex)
	{
		if (hex.length() == 8)
		{
			String roomIP = "";
			roomIP += Integer.valueOf(hex.substring(0, 2), 16) + ".";
			roomIP += Integer.valueOf(hex.substring(2, 4), 16) + ".";
			roomIP += Integer.valueOf(hex.substring(4, 6), 16) + ".";
			roomIP += Integer.valueOf(hex.substring(6, 8), 16);

			return roomIP;
		}
		return "";
	}

	private void connect()
	{
		doc = chat_text.getDocument();
		username = username_text.getText();
		if (legalUsername(username))
			try
			{
				InetAddress inet = InetAddress.getByName(hexToIP(hexip_text.getText()));
				socket = new Socket(inet, 9153);
				DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
				DOS.writeUTF("0" + username);
				connect_button.setEnabled(false);
				username_text.setEnabled(false);
				hexip_text.setEnabled(false);
				hexip_text.setText(hexToIP(hexip_text.getText()));
				users_doc = user_list.getDocument();
				startListening();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		else
		{
			try
			{//todo make red
				StyleContext context = new StyleContext();
				// build a style
				Style style = context.addStyle("test", null);
				// set some style properties
				StyleConstants.setForeground(style, Color.RED);

				doc.insertString(0, "Username is not legal\n", style);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private void disconnect()
	{
		try
		{
			try
			{
				doc.insertString(doc.getLength(), "\nDisconnected", null);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
			DOS.writeUTF("3");
			keepListening = false;
			socket.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void startListening()
	{
		Runnable clientListener = new Runnable()
		{
			@Override public void run()
			{
				while (keepListening)
					try
					{
						String msg_received;

						DataInputStream DIS = new DataInputStream(socket.getInputStream());
						msg_received = DIS.readUTF();
						switch (Character.getNumericValue(msg_received.charAt(0)))//todo use final
						{
						case CONNECT:
							username = msg_received.substring(1);
							send_button.setEnabled(true);
							username_text.setText(username);
							break;

						case NORMAL_MESSAGE:
							if (!msg_received.substring(1, msg_received.indexOf("-")).equals(username))
								try
								{
									doc.insertString(doc.getLength(), "\n" + msg_received.substring(1), null);
								}
								catch (Exception e)
								{
									e.printStackTrace();
								}
							break;

						case PRIVATE_MESSAGE:
							try
							{
								doc.insertString(doc.getLength(), "\n" + msg_received.substring(1), null);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
							break;

						case DISCONNECT:
							disconnect();
							break;

						case STILL_ALIVE:
							break;

						case USER_LIST:
							String[] users = msg_received.substring(1).split("-");

							try
							{
								user_list.setText("");
								for (String user : users)
									users_doc.insertString(doc.getLength(), "\n" + user, null);
								users_doc.insertString(0, "Users:", null);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
							break;

						case ERROR:
							try
							{
								StyleContext context = new StyleContext();
								// build a style
								Style style = context.addStyle("test", null);
								// set some style properties
								StyleConstants.setForeground(style, Color.RED);

								doc.insertString(doc.getLength(), "\n" + msg_received.substring(1) + "\n", style);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
							break;

						default:
							System.err.println("Message not up to code");
						}

						System.out.println(msg_received);
					}
					catch (IOException e)
					{
						System.err.println("Unable to process client request");
						e.printStackTrace();
					}
			}
		};
		Thread clientThread = new Thread(clientListener);
		clientThread.start();
	}

	private void sendMessage()
	{
		String msg = message_text.getText();
		message_text.setText("");
		try
		{
			doc.insertString(doc.getLength(), "\n" + msg, null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		if (msg.length() > 4 && msg.substring(0, 3).equals("pm/") && msg.substring(3).contains("/"))
			//pm/recipient/message
			sendPrivateMessage(msg);
		else
			sendNormalMessage(msg);
	}

	private void sendNormalMessage(String msg)
	{
		try
		{
			DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
			DOS.writeUTF(NORMAL_MESSAGE + username + "-" + msg);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void sendPrivateMessage(String msg)
	{
		int secondSlash = msg.indexOf("/", 3);
		try
		{
			DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
			DOS.writeUTF(PRIVATE_MESSAGE + username + "-" + msg.substring(3, secondSlash) + "-" + msg.substring(secondSlash));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
