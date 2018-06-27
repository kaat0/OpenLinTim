

public class Test
{
	private Test() {}  // class only contains static methods



	public static void main(String[] args) throws Exception
	{
		NonPeriodicEANetwork Net = IO.readNonPeriodicEANetwork(false, false);
	}
}
