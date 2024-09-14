package ca.ubc.cs317.dict.net;

import ca.ubc.cs317.dict.model.Database;
import ca.ubc.cs317.dict.model.Definition;
import ca.ubc.cs317.dict.model.MatchingStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by Jonatan on 2017-09-09.
 */
public class DictionaryConnection {

    private static final int DEFAULT_PORT = 2628;

    //private variables I am defining
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private DictStringParser parser;


    /** Establishes a new connection with a DICT server using an explicit host and port number, and handles initial
     * welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @param port Port number used by the DICT server
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host, int port) throws DictConnectionException {
        // TODO Replace this with code that creates the requested connection
        //"Establish a connection with a DICT server, and receive the initial welcome message."
        if (host.length() == 0) {
            throw new DictConnectionException("Host not provided.");
        }
        if (port < 1000) {
            throw new DictConnectionException("Did not provide port above 1000.");
        }
        try {
            this.socket = new Socket(host, port);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.parser = new DictStringParser();
            String first_welcome_message = in.readLine();
            System.out.println(first_welcome_message);

        } catch (UnknownHostException e) {
            try {
                this.in.close();
                this.out.close();
                this.socket.close();
            } catch (IOException e1) {
                throw new DictConnectionException(e1);
            }
            throw new DictConnectionException();
            

        } catch (IOException e) {
            try {
                this.in.close();
                this.out.close();
                this.socket.close();
            } catch (IOException e1) {
                throw new DictConnectionException(e1);
            }
            throw new DictConnectionException(e.getMessage());
        }
    }

    /** Establishes a new connection with a DICT server using an explicit host, with the default DICT port number, and
     * handles initial welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host) throws DictConnectionException {
        this(host, DEFAULT_PORT);
    }

    /** Sends the final QUIT message and closes the connection with the server. This function ignores any exception that
     * may happen while sending the message, receiving its reply, or closing the connection.
     *
     */
    public synchronized void close() {
        try {
            
            this.out.println("QUIT");
            String last_message = in.readLine();
            System.out.println(last_message);

            this.in.close();
            this.out.close();
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Requests and retrieves all definitions for a specific word.
     *
     * @param word The word whose definition is to be retrieved.
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 definitions in the first database that has a definition for the word should be used
     *                 (database '!').
     * @return A collection of Definition objects containing all definitions returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Collection<Definition> getDefinitions(String word, Database database) throws DictConnectionException {
        Collection<Definition> set = new ArrayList<>();

        // TODO Add your code here

        try {
            this.out.println("DEFINE" + database.getName() + '"' + word + '"');
            String line;
            while (true) {
                line = this.in.readLine();
                String[] split_line = this.parser.splitAtoms(line);

                if (line.equals(".")) {
                    break;
                }
                set.add(new Definition(split_line[0], split_line[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return set;
    }

    /** Requests and retrieves a list of matches for a specific word pattern.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param strategy The strategy to be used to retrieve the list of matches (e.g., prefix, exact).
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 matches in the first database that has a match for the word should be used (database '!').
     * @return A set of word matches returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<String> getMatchList(String word, MatchingStrategy strategy, Database database) throws DictConnectionException {
        Set<String> set = new LinkedHashSet<>();

        // TODO Add your code here

        try {
            this.out.println("MATCH" + database.getName() + strategy + '"' + word + '"');
            String line;
            while (true) {
                line = this.in.readLine();
                if (line.equals(".")) {
                    break;
                }
                if (line.contains())
                set.add(line);
            }
        } catch (IOException e) {
            throw new DictConnectionException(e.getMessage());
        }

        return set;
    }

    /** Requests and retrieves a map of database name to an equivalent database object for all valid databases used in the server.
     *
     * @return A map of Database objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Map<String, Database> getDatabaseList() throws DictConnectionException {
        Map<String, Database> databaseMap = new HashMap<>();

        // TODO Add your code here
        try {
            this.out.println("SHOW DB");
            // String list_of_databases = this.in.readLine();
            while (true) {
                String line = this.in.readLine();
                if (line.equals(".")) { // transmission ends on "."
                    break;
                }
                String[] split_string = parser.splitAtoms(line);
                if (split_string.length < 2 || split_string[0].length() == 0 || split_string[1].length() == 0) {
                    throw new DictConnectionException("Invalid response from dictionary server.");
                }
                if (!split_string[0].contains("-")) {
                    continue;
                }
                databaseMap.put(split_string[0], new Database(split_string[0], split_string[1]));
            }
           

        } catch (IOException e) {
            throw new DictConnectionException(e);
        }
       
        return databaseMap;
    }

    /** Requests and retrieves a list of all valid matching strategies supported by the server.
     *
     * @return A set of MatchingStrategy objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
        Set<MatchingStrategy> set = new LinkedHashSet<>();

        try {
            this.out.println("SHOW STRAT");

            while (true) {
                String line = this.in.readLine();
                if (line.equals(".")) {break;}
                String[] split_line = this.parser.splitAtoms(line);
                String name = split_line[0];
                String description = split_line[1];

                if (name.length() == 0 || description.length() == 0) {
                    throw new DictConnectionException("Name or description not provided.");
                }
                set.add(new MatchingStrategy(split_line[0], split_line[1]));
            }
        } catch (IOException e) {
            throw new DictConnectionException(e);
        }

        return set;
    }

    /** Requests and retrieves detailed information about the currently selected database.
     *
     * @return A string containing the information returned by the server in response to a "SHOW INFO <db>" command.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized String getDatabaseInfo(Database d) throws DictConnectionException {
	StringBuilder sb = new StringBuilder();
    try {
        while (true) {
            out.println("SHOW INFO " + d.getName());
            String line = this.in.readLine();
            if (line.equals(".")) {
                break;
            }
            sb.append(line);

        }
    } catch (IOException e) {
        throw  new DictConnectionException(e);
    }

        return sb.toString();
    }
}
