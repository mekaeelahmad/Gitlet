package gitlet;

import java.io.File;


/** Driver class for Gitlet, a subset of the Git version-control system.
 * Main method for Gitlet. Takes input commands from user and allocates
 * the command into its respective method that is in the repository.
 *  @author Mekaeel Ahmad, Vinay Agrawal
 */
public class Main {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = Utils.join(CWD, ".gitlet");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */

    /**Main method that takes in input from a user and sends that command
     * to a method that is in the Repo class.
     * @param args input from the user that is a list of strings*/
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                if (GITLET_DIR.exists()) {
                    System.out.println("A Gitlet version-control system "
                            + "already " + "exists in the current directory.");
                } else {
                    Repository.init();
                }
                break;
            case "add":
                if (!checkValid()) {
                    break;
                }
                if (args.length == 2) {
                    Repository.add(args[1]);
                } else {
                    System.out.println("Incorrect operands. ");
                }
                break;
            case "commit":
                if (!checkValid()) {
                    break;
                }
                if (args[1].equals("") || args.length == 1) {
                    System.out.println("Please enter a commit message.");
                } else if (args.length == 2) {
                    Repository.commit(args[1], false);
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            case "rm":
                if (!checkValid()) {
                    break;
                }
                if (args.length == 2) {
                    Repository.remove(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            default: mainHelperHelper(args);
        }
        return;
    }
    /**Helper for main method. */
    public static void mainHelperHelper(String [] args) {
        String firstArg = args[0];
        switch (firstArg) {
            case "log":
                if (!checkValid()) {
                    break;
                }
                if (args.length == 1) {
                    Repository.log();
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            case "global-log":
                if (!checkValid()) {
                    break;
                }
                if (args.length == 1) {
                    Repository.globalLog();
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            case "find":
                if (!checkValid()) {
                    break;
                }
                if (args.length == 2) {
                    Repository.find(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            case "status":
                if (!checkValid()) {
                    break;
                }
                if (args.length == 1) {
                    Repository.status();
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            case "merge":
                if (!checkValid()) {
                    break;
                }
                if (args.length == 2) {
                    Repository.merge(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            default: mainHelper(args);
        }
    }
    /**Another helper for main method. */
    public static void mainHelper(String [] args) {
        String firstArg = args[0];
        switch (firstArg) {
            case "checkout":
                if (!checkValid()) {
                    break;
                }
                if (args.length == 3 && args[1].equals("--")) {
                    Repository.checkoutFile(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    Repository.checkoutIdFile(args[1], args[3]);
                } else if (args.length == 2) {
                    Repository.checkoutBranch(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            case "branch":
                if (!checkValid()) {
                    break;
                }
                if (args.length == 2) {
                    Repository.branch(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            case "rm-branch":
                if (!checkValid()) {
                    break;
                }
                if (args.length == 2) {
                    Repository.removeBranch(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            case "reset":
                if (!checkValid()) {
                    break;
                }
                if (args.length == 2) {
                    Repository.reset(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            default:
                System.out.println("No command with that name exists.");
        }

    }
    /**Checks whether or not the Gitlet directory exists. */
    public static boolean checkValid() {
        if (GITLET_DIR.exists()) {
            return true;
        } else {
            System.out.println("Not in an initialized Gitlet directory. ");
            return false;
        }
    }
}
