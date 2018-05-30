import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main
{

	public static void main(String[] args)
	{
		String msg = "pm/david/hello";
		int secondSlash = msg.indexOf("/",3);
		System.out.println("ofir" + "-" + msg.substring(3, secondSlash) + "-" + msg.substring(secondSlash));
	}
}
