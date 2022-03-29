# matrix-hangbot
desc  

## configuring
Edit Main.java in your favorite text editor and edit the `homeserverUrl` and `accessToken` variables to your chosen values. The former variable will be your server domain, i.e. `matrix.org`. For info on the latter, see <https://t2bot.io/docs/access_tokens/>  

## building (under UNIX)
To build, clone these two repos below:  
<https://github.com/JojiiOfficial/Matrix-ClientServer-API-java>  
<https://github.com/stleary/JSON-java>  

To build them with maven, run `mvn compile` in both of the above repos.  
Then you'll want to run `cp -r ./target/classes/* /path/to/cloned/matrix-hangbot`

Then under matrix-hangbot, you can compile with a simple `javac Main.java` and run with a simpler `java Main`

## TODO
more stringent checks on given numbers and other values so I don't get pwned
fix json message output so code blocks look proper
make it require the bot and the sender to be in a roomID if the user specifies one
figure out if read receipts work
make an alert for when a game has been started in another room
make a status command
