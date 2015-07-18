class TextNumPrinter
{
	public static void main(String[] args)
	{
		//any string of the form "\u001b[*ANYTHING*m is formatting. formatting tells whether we're currently parsing formatting, and inFormattedArea should be
		//pretty self-explanatory.
		boolean formatting = false;
		boolean outputModeColoredRepeat = false;
		if(args.length > 1)
		{
			outputModeColoredRepeat = args[1].toLowerCase().equals("o");
		}
		boolean inFormattedArea = false;
		String escaped = new String();
		char escapeChar = (char)27;
		String resetString = "[0m";
		breakout:
		while(true)
		{
			try
			{
				int b = System.in.read();
				if(outputModeColoredRepeat)
				{
				switch(b)
				{
					case -1: //stream is ended
					break breakout;

					case 94: //^
					if(!formatting) formatting = true; escaped = new String(new char[]{escapeChar}); break;

					case 27: //escape
					if(!formatting) formatting = true; escaped = new String(new char[]{escapeChar}); break;

					case 109: //m
					if(formatting)
					{
						formatting = false;
						escaped = escaped + "m";
						System.out.print(escaped);
						if(escaped.contains(resetString) && inFormattedArea == true)
						{
							inFormattedArea = false;
						}else
						{
							inFormattedArea = true;
						}
						break;
					}

					case 10: //line break
					if(!inFormattedArea)
					{
						System.out.print("\n");
						System.out.print("time: " + System.currentTimeMillis() + "   ");
						break;
					}

					default:
					if(formatting) escaped = escaped + (char)b;
					else System.out.print(b + " ");
				}
				}else
				{
					
				}

			}catch(Exception e)
			{
				System.out.println("OOPS " + e.getMessage());
			}
		}
		System.out.print("\n");
	}
}
