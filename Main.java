import de.jojii.matrixclientserver.Bot.*;
import de.jojii.matrixclientserver.Bot.Events.*;
import org.json.*;
import java.io.*;
import java.util.*;

public class Main{
	// URL of your homeserver. https not yet supported in Jojii's library
	static final String homeserverUrl = "http://matrix.org";

	// Access token, see the matrix documentation for more info
	static final String accessToken = "put your access token here, remember to remove it if pushing to git";

	// The map, where the room ID is the key, and the value contains a nesed map of the word to guess, how much of the word is currently guessed, and how many guesses are left
	static Map<String, Map> map;
	// The nested map's key is an Int, and value is a String. 1 is the word to be guessed, 2 is how much is currently guessed of the word, 3 is current # of guesses, 4 is the amount of guesses specified, 5 is how many turns they've used

	public static void main(String[] args){
		map = new HashMap<String, Map>();//instantiate the main var
		// login
		Client c = new Client(homeserverUrl);
		try {
			c.login(accessToken, loginData -> {
				if (loginData.isSuccess()) {
					System.out.println("client logged in!");
					botPostLogin(c);//this calls the below function
				} else {
					System.err.println("error logging in");
				}
			});
		} catch (IOException e){
			e.printStackTrace();
		}
	}

	private static void botPostLogin(Client c){
		System.out.println("botpostlogin");//REMOVE
		c.registerRoomEventListener(roomEvents -> {System.out.println("ruervui");});//remoe
		c.registerRoomEventListener(roomEvents -> {
			System.out.println("roomeventlistener");//REMOVE
			for (RoomEvent e : roomEvents){//loop for code to execute on each 'event'
				//log all events raw to stdout
				System.out.println(e.getRaw().toString());

				//bot autojoin
				if(e.getType().equals("m.room.member") && e.getContent().has("membership") && e.getContent().getString("membership").equals("invite")){
					try{
					c.joinRoom(e.getRoom_id(), null);} catch (IOException x) {x.printStackTrace();}
				}

				//read receipts
				else if (e.getType().equals("m.room.message")){
					try{
					c.sendReadReceipt(e.getRoom_id(), e.getEvent_id(), "m.read", null);} catch (IOException x) {x.printStackTrace();}
				}

				//parsing for !hang
				if (e.getContent().has("body")){
					//m is equal to the message body
					String m = RoomEvent.getBodyFromMessageEvent(e);
					if (m != null && m.trim().length() > 0){
						if (m.length() > 4 && m.substring(0, 5).equals("!hang")){
							System.out.println("hang detected, here is the message body:" + m);//REMOVE

							//testing for blankness after command or for `!hang help` to show useage info
							if (!(m.length() > 6) || (m.length() > 9 && m.substring(6, m.length()).equals("help")))
								sendMessage(getCopyPasta("help"), c, e);//sends help & useage info as a message
							else{
								try{
									//change m to just the content of the message without the initial `!hang `
									m = m.substring(6, m.length());
									//create map to be able to be put into the main map var (this will end up being the nested one)
									Map<Integer, String> nestedMap = new HashMap<Integer, String>();

									// 'sub' would be set equal to the first "command" in m, i.e. 'new', 'newnumext', etc. separated by a space.
									// The reason we are doing this check is becuase on guess there is only one "command" and otherwise 
									// an IndexOutOfBounds exception would be thrown as there would be no space in m
									String sub;
									if(m.indexOf(" ") != -1){
										sub = m.substring(0, m.indexOf(" "));
									}
									else{
										sub = "default";
									}

									// Variable instantiation fuckery because switches are weird
									String mm = m;//in the case of a guess, m will be set back to this
									String uno;//for cases where there are multiple arguments (newnum, newext, newnumext), this will be the first of them
									String roomID;

									//bring down m (message) to next thing after sub(command)
									m = m.substring(m.indexOf(" ")+1, m.length());//this doesn't throw an exception with a guess, as finding the index of a space will return -1, which +1 is 0

									switch(sub){
										case "new":
											nestedMap.put(1, dashesToSpaces(m.toLowerCase())); //sets word to lowercase and makes dashes into spaces
											nestedMap.put(2, nestedMap.get(1).replaceAll("[^ ]", "-"));//makes everything but spaces blanks, will be filled in by guesses
											map.put(e.getRoom_id(), nestedMap);
											//sendMessage(nestedMap.get(1) + " " + nestedMap.get(2), c, e);
											sendMessage("Word set, guess ahead! " + nestedMap.get(2), c, e);
											break;
										case "newext":
											//uno is just the word that will be guessed
											uno = m.substring(0, m.indexOf(" "));
											nestedMap.put(1, dashesToSpaces(uno.toLowerCase()));
											nestedMap.put(2, nestedMap.get(1).replaceAll("[^ ]", "-"));
											//m becomes the specified room ID
											m = m.substring(m.indexOf(" ")+1, m.length());
											map.put(m, nestedMap);
											sendMessage("Game started in: " + m + "\nYou picked the word: " + nestedMap.get(1), c, e);
											break;
										case "newnum":
											uno = m.substring(0, m.indexOf(" "));
											nestedMap.put(1, dashesToSpaces(uno.toLowerCase()));
											nestedMap.put(2, nestedMap.get(1).replaceAll("[^ ]", "-"));

											//the number of guesses
											m = m.substring(m.indexOf(" ")+1, m.length());
											if(!(Integer.parseInt(m) > 0)){
												sendMessage(getCopyPasta("badnum"), c, e);
												break;
											}
											nestedMap.put(3, m);
											nestedMap.put(4, m);
											map.put(e.getRoom_id(), nestedMap);
											sendMessage("Number of guesses: " + m + "\nGuess ahead! " + nestedMap.get(2), c, e);
											break;
										case "newnumext":
											//the first term, the word to be guessed
											uno = m.substring(0, m.indexOf(" "));
											nestedMap.put(1, dashesToSpaces(uno.toLowerCase()));
											nestedMap.put(2, nestedMap.get(1).replaceAll("[^ ]", "-"));

											//the second term, the number of guesses
											String dos = m.substring(m.indexOf(" ")+1, m.indexOf(" ", m.indexOf(" ") + 1));
											if(!(Integer.parseInt(dos) > 0)){
												sendMessage(getCopyPasta("badnum"), c, e);
												break;
											}
											nestedMap.put(3, dos);
											nestedMap.put(4, dos);

											//the third term, the roomID for the word
											roomID = m.substring(m.indexOf(" ", m.indexOf(" ") + 1)+1, m.length());

											//put our created map to be nested inside the main 'map' var
											map.put(roomID, nestedMap);
											//sendMessage(nestedMap.get(1) + " " + nestedMap.get(2) + " " + nestedMap.get(3) + " " + nestedMap.get(4) + " " + uno + " " + dos + " " + roomID, c, e);
											sendMessage("You picked the word: " + nestedMap.get(1) + "\nWith this many guesses: " + nestedMap.get(4) + "\nAnd in this room: " + roomID + "\nHave fun!", c, e);

											break;
										case "help":
											sendMessage(getCopyPasta("help"), c, e);
											break;
										case "status":
											if(map.get(e.getRoom_id()).get(3) != null)
												sendMessage(map.get(e.getRoom_id()).get(2) + "\nGuesses left: " + map.get(e.getRoom_id()).get(3), c, e);
											else
												sendMessage(map.get(e.getRoom_id()).get(2) + "", c, e);//adding empty quotes is a shorter way to cast to a String
											break;
										default:// The default case is for a guess
											//replaces all dashes into spaces and lowercases all of the user's guess
											m = mm;
											m = dashesToSpaces(m.toLowerCase());

											//Setting getRoom_id() into a String var here because casting it repeatedly is a pain
											roomID = ((String) e.getRoom_id());

											//This var (editMap) is going to point to reference the exact same object as the nested map: any changes to it change the nested map of <Integer, String>
											Map<Integer, String> editMap = map.get(roomID);

											//String with the value of my fillGuess method below
											String filledGuess = fillGuess(editMap.get(2), m, editMap.get(1));

											//Checking if their guess won the game
											if(m.equals(editMap.get(1)) || editMap.get(1).equals(filledGuess) || editMap.get(1).equals(editMap.get(2))){
												sendMessage(getCopyPasta("victoryFullGuess") + editMap.get(1), c, e);//maybe I should show how many guesses were left?
												editMap.clear();
												break;
											}

											//Checking if their guess is in the word
											if((editMap.get(1).contains(m) || !(editMap.get(2).equals(filledGuess))) && editMap.get(2) != filledGuess){//can guess any amount of consecutive letters aorn
												editMap.put(2, filledGuess);
												sendMessage(m + getCopyPasta("victoryPartialGuess") + filledGuess, c, e);
											}

											else{
												//sendMessage("guess not in the word" + m + "  " + roomID + "  " + editMap.get(1) + filledGuess, c, e);
												if(editMap.get(2).contains(m)){
													sendMessage("...er- t-that's already on the board? Question mark?", c, e);
												}
												messageGuessesLeft(editMap, c, e);
											}
											break;
									}
								} catch (IndexOutOfBoundsException oob){
									sendMessage(getCopyPasta("badsyntax"), c, e);
									oob.printStackTrace();
								} catch (NullPointerException npe){
									sendMessage(getCopyPasta("nullpointer"), c, e);
									npe.printStackTrace();
								}
							}
						}
					}
				}
			}//end of loop
		});//end of event listener
	}

	// Function to send a message. This function sends a message as 'plain' without markdown support
	//TODO: make an overloaded method to add another input for a formatted string
	private static void sendMessage(String s, Client c, RoomEvent e){
		try{
			c.sendText(e.getRoom_id(), s, null);
		} catch (Exception y) {System.out.print(y);}
	}

	//returns a string with spaces removed, not rocket science
	private static String removeSpaces(String s){
		return s.replaceAll("\\s", "");
	}
	//returns a string with dashes removed
	private static String removeDashes(String s){
		return s.replaceAll("-", "");
	}

	//returns a string with spaces replaced by dashes
	private static String spacesToDashes(String s){
		return s.replaceAll("\\s", "-");
	}
	//returns a string with dashes replaces by spaces
	private static String dashesToSpaces(String s){
		return s.replaceAll("-", " ");
	}

	//fills in nestedMap[2] (current word guessing progress) with a letter or phrase
	private static String fillGuess(String dashes, String guess, String word){
		String finalString = "";
		//make sure to turn dashes into spaces and turn lowercase for guess
		System.out.println(dashes + guess);//REMOVE
		for(int i = 0; i < dashes.length(); i++){
			System.out.println(i + " " + finalString);//REMOVE
			if(i + guess.length() <= word.length() && word.substring(i, i + guess.length()).equals(guess)){
				finalString += guess;
				i += (guess.length() - 1);
			}
			else{
				//finalString += "-"; The reason we're using the below and not this is because the String dashes also contains spaces
				finalString += dashes.substring(i, i + 1);
			}
		}
		if(finalString.length() == dashes.length()){
			return finalString;
		}
		System.err.println("%%%%%%% finalString and dashes are not the same length %%%%%%%%");
		return "Error while calculating guess: internal mismatch of string lengths.\nIf you encountered this naturally, please file a bug at Richard-Rogalski/matrix-hangbot";
	}

	// Sends a message with number of guesses left, if applicable. Also returns the number of guesses left as an int
	private static int messageGuessesLeft(Map<Integer, String> map, Client c, RoomEvent e){
		if(map.containsKey(3) && map.get(3) != null && map.get(3).length() > 0){
			Integer n = Integer.parseInt(map.get(3));
			n--;
			map.put(3, n.toString());
			if(n > 0){
				sendMessage("You have " + map.get(3) + " guesses left.", c, e);
			}
			else{
				sendMessage("Out of guesses :(, better luck next time!", c, e);
				map.clear();
			}
			return n;
		}
		return -1;
	}

	// This was my idea to keep large text blocks at the end of the file instead of in the middle, as I found that would be easier to work with.
	// Whatever value is given to this function returns the corresponding case
	private static String getCopyPasta(String s){
		switch(s){
			case "help":
				return "Hangbot Useage: \n<code>!hang new <wordtoguess></code> to start a new game.\n`!hang newext <wordtoguess> <roomID>` to start a new game in a room other than the one you're currently using.\n`!hang newnum <wordtoguess> <maxguesses>` to start a game with a maximum amount of guesses.\n`!hang newnumext <wordtoguess> <maxguesses> <roomID>` to start a game in another room with a max amount of guesses.\n`!hang help` shows this page.\n\nTo pick a random word, you can substitute an asterisk (*) in the spot you'd otherwise choose a word to be guessed.\n\nTo guess and actually play once a game has begun, simply: `!hang <yourguess>`.(you can guess either an individual letter, or the word in full)";
			case "badchar":
				return "Illegal character used. Illegal characters: * / \\ \nDashes (-) are allowed, but will be treated as spaces.";
			case "badsyntax":
				return "Syntax not valid. Run\n`!hang help`\nfor help.";
			case "nullpointer"://note: I should probably change the below
				return "Error: null pointer thrown. You were probably trying to guess a word before a word has been set for your room. For help, run\n`!hang help`";
			case "badnum":
				return "Sorry, number is invalid. Please type the number itself (5) instead of typing it out (five). Also, no you cannot choose 0 or a negative.";
			case "0guesses":
				return "Sorry, you have used up all of your guesses. Feel free to try again!";
			case "dupguess":
				return "You have already guessed that letter.";
			case "wrongguess"://begins with the guessed value
				return " : Is an incorrect guess. Sorry, guess again. ";
			case "victoryFullGuess"://ends with the full word
				return "Congratulations! You guessed right! The full word is: ";
			case "victoryPartialGuess"://begins with the guessed value
				return " : Was a correct guess! Your current progress is now:\n";
			default:
				return "Incorrect copypasta. If you encountered this message naturally please file a bug at Richard-Rogalski/matrix-hangbot";
		}
	}
}
