package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;




/** Represents a gitlet repository.
 *  does at a high level.
 *  Initialized all important directory paths and contains all gitlet commands
 *  @author Vinay Agrawal and Makael Ahmed
 */
public class Repository {


    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = Utils.join(CWD, ".gitlet");

    /** Blob directory. */
    public static final File BLOB_DIR = Utils.join(GITLET_DIR, "blobs");

    /** Commit Directory. */
    public static final File COMMIT_DIR = Utils.join(GITLET_DIR, "commits");

    /** Staging Directories. */
    public static final File STAGING_DIR = Utils.join(GITLET_DIR, "stage");

    /** Addition Directory. */
    public static final File ADDITION_DIR = Utils.join(STAGING_DIR, "add");

    /** Removal Directory. */
    public static final File REMOVAL_DIR = Utils.join(STAGING_DIR, "rem");

    /** Branch Directory. */
    public static final File BRANCH_DIR = Utils.join(GITLET_DIR, "branches");

    /** Reference to Head commit. */
    public static final File HEAD = Utils.join(GITLET_DIR, "HEAD");

    /** Reference to current branch. */
    public static final File MAIN = Utils.join(GITLET_DIR, "MAIN");

    /** Creates necessary gitlet directories and the initial commit. */
    public static void init() {
        GITLET_DIR.mkdir();
        STAGING_DIR.mkdir();
        ADDITION_DIR.mkdir();
        REMOVAL_DIR.mkdir();
        BLOB_DIR.mkdir();
        COMMIT_DIR.mkdir();
        BRANCH_DIR.mkdir();
        File main = Utils.join(BRANCH_DIR, "main");
        try {
            HEAD.createNewFile();
            MAIN.createNewFile();
            main.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String initCommit = Commit.create("initial commit",
                null, null, new HashMap<>());
        Utils.writeContents(HEAD, initCommit);
        Utils.writeContents(MAIN, "main");
        Utils.writeContents(main, initCommit);
    }

    /** Commits all files in staging area with given message.
     * If no files staged, exists with error message.
     * @param message description of commit
     * @param merge true if creating a merge commit
     */
    public static void commit(String message, boolean merge) {
        List<String> stagedAdd = Utils.plainFilenamesIn(ADDITION_DIR);
        List<String> stagedRem = Utils.plainFilenamesIn(REMOVAL_DIR);
        if (stagedAdd.size() == 0 && stagedRem.size() == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }

        String parent1 = Utils.readContentsAsString(HEAD);
        Commit parentCommit = Commit.read(parent1);
        HashMap<String, String> prevBlobs = parentCommit.getBlobs();
        for (String f: stagedAdd) {
            File staged = Utils.join(ADDITION_DIR, f);
            prevBlobs.put(f, Utils.readContentsAsString(staged));
        }
        for (String f: stagedRem) {
            prevBlobs.remove(f);
        }
        clearStage();
        String newHead;
        if (merge) {
            File branch = Utils.join(BRANCH_DIR, message);
            String parent2 = Utils.readContentsAsString(branch);
            String newMess = "Merged " + message + " into " + getMain() + ".";
            newHead = Commit.create(newMess, parent1, parent2, prevBlobs);
        } else {
            newHead = Commit.create(message, parent1, null, prevBlobs);
        }
        Utils.writeContents(HEAD, newHead);
        Utils.writeContents(Utils.join(BRANCH_DIR, getMain()), newHead);
    }

    /** Calls commit method with merge branch.
     * @param otherBranch branch that is merged
     */
    private static void commitMerge(String otherBranch) {
        File other = Utils.join(BRANCH_DIR, otherBranch);
        if (other.exists()) {
            commit(otherBranch, true);
        } else {
            System.out.println("Merge branch does not exist. ");
        }
        return;
    }

    /** Clears all files from staging area. */
    private static void clearStage() {
        List<String> addFiles = Utils.plainFilenamesIn(ADDITION_DIR);
        List<String> remFiles = Utils.plainFilenamesIn(REMOVAL_DIR);
        for (String f: addFiles) {
            Utils.join(ADDITION_DIR, f).delete();
        }
        for (String f: remFiles) {
            Utils.join(REMOVAL_DIR, f).delete();
        }
        return;
    }

    /** Returns the name of the current branch.
     * @return returns the name of the current branch */
    private static String getMain() {
        return Utils.readContentsAsString(MAIN);
    }

    /** Add command attempts to add file to staging area.
     * If file does not exist, prints error message.
     * @param file file to add to staging area
     */
    public static void add(String file) {
        File state1 = Utils.join(REMOVAL_DIR, file);
        File state2 = Utils.join(ADDITION_DIR, file);
        File state3 = Utils.join(CWD, file);
        if (!state3.exists()) {
            System.out.println("File does not exist. ");
            return;
        }
        Commit headCommit = Commit.read(getHead());
        String prevBlob = headCommit.getFile(file);
        String newBlob = Blob.create(file, Utils.readContents(state3));
        if (state1.exists()) {
            state1.delete();
        }
        if (!newBlob.equals(prevBlob)) {
            Utils.writeContents(state2, newBlob);
        }
    }

    /** Returns contents of HEAD file.
     * @return returns sha1 of HEAD commit
     */
    private static String getHead() {
        return Utils.readContentsAsString(HEAD);
    }

    /** Unstage the file if it is currently staged for addition.
     * If the file is tracked in the current commit, stage it for removal
     * and remove the file from the working directory
     * if the user has not already done so.
     * Do not remove it unless it is tracked in the current commit.
     * @param file file to remove to staging area for removal
     */
    public static void remove(String file) {
        File remFile = Utils.join(ADDITION_DIR, file);
        Commit headCommit = Commit.read(getHead());
        String blobId = headCommit.getFile(file);
        if (!remFile.exists() && blobId == null) {
            System.out.println("No reason to remove the file.");
            return;
        }
        if (remFile.exists()) {
            remFile.delete();
        }

        if (blobId != null) {
            File fileForRemoval = Utils.join(REMOVAL_DIR, file);
            Utils.writeContents(fileForRemoval, blobId);
            if (Utils.join(CWD, file).exists()) {
                Utils.join(CWD, file).delete();
            }
        }
    }

    /** Prints commit history of current HEAD Commit. */
    public static void log() {
        String headId = Utils.readContentsAsString(HEAD);
        Commit headCommit = Commit.read(headId);
        for (Commit commit: headCommit) {
            System.out.println(commit);
        }
        return;
    }

    /** Prints ALL of commit history. */
    public static void globalLog() {
        List<String> commitHist = Utils.plainFilenamesIn(COMMIT_DIR);
        for (String id: commitHist) {
            Commit commit = Commit.read(id);
            System.out.println(commit);
        }
        return;
    }

    /** Print sha1 of all commits with given commit message.
     * @param message commit message that is being searched for.
     */
    public static void find(String message) {
        List<String> commitHist = Utils.plainFilenamesIn(COMMIT_DIR);
        int count = 0;
        for (String id: commitHist) {
            Commit commit = Commit.read(id);
            if (commit.getMessage().equals(message)) {
                System.out.println(commit.getId());
                count += 1;
            }
        }
        if (count == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Displays what branches currently exist,
     * and marks the current branch with an asterik.
     * Also displays what files have been staged for addition or removal.
     */
    public static void status() {
        System.out.println("=== Branches ===");
        for (String f: Utils.plainFilenamesIn(BRANCH_DIR)) {
            if (f.equals(getMain())) {
                System.out.print("*");
            }
            System.out.println(f);
        }
        System.out.println("\n=== Staged Files ===");
        for (String f: Utils.plainFilenamesIn(ADDITION_DIR)) {
            System.out.println(f);
        }
        System.out.println("\n=== Removed Files ===");
        for (String f: Utils.plainFilenamesIn(REMOVAL_DIR)) {
            System.out.println(f);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        System.out.println("\n=== Untracked Files ===");
    }


    /** Takes the version of the file as it exists in the head commit
     * and puts it in the working directory, overwriting the version
     * of the file that’s already there if there is one.
     * The new version of the file is not staged.
     * @param targetFile name of the file to checkout
     */
    public static void checkoutFile(String targetFile) {
        checkoutIdFile(getHead(), targetFile);
        return;
    }

    /** Takes the version of the file in the commit with the given id,
     * and puts it in the working directory, overwriting the version of the file
     * that’s already there. The new version of the file is not staged.
     * @param targetFile name of file to checkout
     * @param id sha1 of commit
     */
    public static void checkoutIdFile(String id, String targetFile) {
        Commit commit = Commit.read(id);
        if (commit != null) {
            String targetBlobId = commit.getFile(targetFile);
            if (targetBlobId != null) {
                byte[] targetBlobContents = Blob.readBlob(targetBlobId);
                File targetLoc = Utils.join(CWD, targetFile);
                Utils.writeContents(targetLoc, targetBlobContents);
            } else {
                System.out.println("File does not exist in that commit.");
            }
        } else {
            System.out.println("No commit with that id exists.");
        }
    }

    /** Creates a new branch with the given name, and points it
     * at the current head commit. A branch is nothing more than
     * a name for a reference (a SHA-1 identifier) to a commit node.
     * @param branch name of new branch
     */
    public static void branch(String branch) {
        File newBranch = Utils.join(BRANCH_DIR, branch);
        if (newBranch.exists()) {
            System.out.println("A branch with that name already exists.");
        }
        Utils.writeContents(newBranch, getHead());
    }

    /** Takes all files in the commit at the head of the given branch,
     * and puts them in the working directory, overwriting the versions of the
     * files that are already there if they exist. At the end of this command,
     * the given branch will now be considered the current branch (HEAD).
     * Any files that are tracked in the current branch but are not
     * present in the checked-out branch are deleted.
     * The staging area is cleared, unless the checked-out branch
     * is the current branch
     * @param branch name of branch to switch to
     */
    public static void checkoutBranch(String branch) {
        File nextBranch = Utils.join(BRANCH_DIR, branch);
        if (getMain().equals(branch)) {
            System.out.println("No need to checkout the current branch. ");
        } else if (!nextBranch.exists()) {
            System.out.println("No such branch exists. ");
        } else {
            String nextHeadId = Utils.readContentsAsString(nextBranch);
            if (!checkout(nextHeadId)) {
                return;
            }
            changeMain(branch);
        }
    }

    /** Checkout all files at the given commit and change head.
     * Errors if untracked files are overwritten.
     * @param nextCommitId commit to be checked out
     * @return Returns true if successful
     */
    private static boolean checkout(String nextCommitId) {
        if (!Commit.exists(nextCommitId)) {
            System.out.println("No commit with that id exists. ");
            return false;
        }
        Commit nextCommit = Commit.read(nextCommitId);
        List<String> untracked = getUntracked();
        HashMap<String, String> blobs = nextCommit.getBlobs();
        for (String fileName: untracked) {
            if (nextCommit.getFile(fileName) != null) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return false;
            }
        }
        List<String> filesCWD = Utils.plainFilenamesIn(CWD);
        for (String f: filesCWD) {
            Utils.restrictedDelete(Utils.join(CWD, f));
        }
        Set<String> fileNames = blobs.keySet();
        for (String name: fileNames) {
            Utils.writeContents(Utils.join(CWD, name), Blob.readBlob(blobs.get(name)));
        }
        changeHead(nextCommitId);
        clearStage();
        return true;
    }

    /** Returns list of untracked files in CWD.
     * @return List of untracked files in CWD.
     */
    private static List<String> getUntracked() {
        Commit headCommit = Commit.read(getHead());
        ArrayList<String> untracked = new ArrayList<>();
        for (String f: Utils.plainFilenamesIn(CWD)) {
            if (headCommit.getFile(f) == null) {
                untracked.add(f);
            }
        }
        return untracked;
    }


    /** Changes head commit.
     * @param id sha1 id of next head commit
     */
    private static void changeHead(String id) {
        Utils.writeContents(HEAD, id);
    }

    /** Changes curr branch.
     * @param branch name of new current branch
     */
    private static void changeMain(String branch) {
        Utils.writeContents(MAIN, branch);
    }

    /** Deletes the branch with the given name.
     * This only means to delete the pointer associated with the branch;
     * it does not mean to delete all commits
     * that were created under the branch.
     * @param branch branch to be removed
     */
    public static void removeBranch(String branch) {
        File thisBranch = Utils.join(BRANCH_DIR, branch);
        if (!thisBranch.exists()) {
            System.out.println("A branch with that name does not exist.");
        } else if (Utils.readContentsAsString(thisBranch).equals(getHead())) {
            System.out.println("Cannot remove the current branch.");
        } else {
            thisBranch.delete();
        }
    }

    /** Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch’s head to that commit node.
     * The [commit id] may be abbreviated as for checkout.
     * The staging area is cleared.
     * @param id sha1 value of commit
     */
    public static void reset(String id) {
        if (checkout(id)) {
            changeBranch(id);
        }
    }

    /** Changes head commit of current branch.
     * @param newBranchHead sha1 id of new head commit.
     * @returns true if operation successful
     */
    private static boolean changeBranch(String newBranchHead) {
        File branchToChange = Utils.join(BRANCH_DIR, getMain());
        if (branchToChange.exists()) {
            Utils.writeContents(branchToChange, newBranchHead);
            return true;
        } else {
            return false;
        }
    }

    /** Merges current branch with this branch.
     * Prints error message if unsuccessful.
     * @param branch branch to merge
     */
    public static void merge(String branch) {
        File otherBranch = Utils.join(BRANCH_DIR, branch);
        List<String> stagedAdd = Utils.plainFilenamesIn(ADDITION_DIR);
        List<String> stagedRem = Utils.plainFilenamesIn(REMOVAL_DIR);
        if (stagedAdd.size() > 0 || stagedRem.size() > 0) {
            System.out.println("You have uncommitted changes. ");
        } else if (branch.equals(getMain())) {
            System.out.println("Cannot merge a branch with itself. ");
        } else if (!otherBranch.exists()) {
            System.out.println("A branch with that name does not exist. ");
        } else if (getUntracked().size() != 0) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first. ");
        } else {
            Commit currCommit = Commit.read(getHead());
            Commit givenCommit = Commit.read(Utils.readContentsAsString(otherBranch));
            Commit splitCommit = findSplit(currCommit, givenCommit);
            if (splitCommit.getId().equals(givenCommit.getId())) {
                System.out.println("Given branch is an ancestor of the current branch.");
            } else if (splitCommit.getId().equals(currCommit.getId())) {
                checkoutBranch(branch);
                System.out.println("Current branch fast-forwarded.");
            } else {
                Set<String> files = Commit.concatenateFiles(currCommit, givenCommit, splitCommit);
                boolean mergeConflict = false;
                for (String f: files) {
                    boolean fileInSplit = splitCommit.tracks(f);
                    boolean fileInCurrent = currCommit.tracks(f);
                    boolean fileInGiven = givenCommit.tracks(f);
                    boolean splitCurrDiff = isDifferent(splitCommit, currCommit, f);
                    boolean splitGivDiff = isDifferent(splitCommit, givenCommit, f);
                    boolean currGiveDiff = isDifferent(givenCommit, currCommit, f);
                    if (!fileInSplit && !fileInCurrent && fileInGiven) {
                        checkoutIdFile(givenCommit.getId(), f);
                        add(f);
                    } else if (!fileInSplit && fileInCurrent && !fileInGiven) {
                        continue;
                    } else if (fileInSplit && fileInCurrent && fileInGiven
                            && !splitCurrDiff && splitGivDiff) {
                        checkoutIdFile(givenCommit.getId(), f);
                        add(f);
                    } else if (fileInSplit && fileInCurrent && fileInGiven
                            && !splitGivDiff && splitCurrDiff) {
                        continue;
                    }  else if (fileInSplit && !splitCurrDiff && !fileInGiven) {
                        remove(f);
                    } else if (fileInSplit && !splitGivDiff && !fileInCurrent) {
                        continue;
                    } else if (!currGiveDiff) {
                        continue;
                    } else {
                        mergeConflict = true;
                        byte[] givenContents = new byte[0];
                        byte[] currContents = new byte[0];
                        if (fileInGiven) {
                            givenContents = Blob.readBlob(givenCommit.getFile(f));
                        }
                        if (fileInCurrent) {
                            currContents = Blob.readBlob(currCommit.getFile(f));
                        }
                        File fileLoc = Utils.join(CWD, f);
                        Utils.writeContents(fileLoc, "<<<<<<< HEAD\n",
                                currContents, "=======\n", givenContents, ">>>>>>>", "\n");
                        add(f);
                    }
                }
                commitMerge(branch);
                if (mergeConflict) {
                    System.out.println("Encountered a merge conflict.");
                }
            }
        }
        return;
    }

    /** Returns true if contents of file in two commits is different.
     * null == null returns true and null == Object returns false;
     * @param c1 first commit to compare
     * @param c2 second commit to compare
     * @param file file to compare
     * @return returns true if fill contents are the same */
    private static boolean isDifferent(Commit c1, Commit c2, String file) {
        return c1.isDifferent(c2, file);
    }

    /** Finds commit at split point between branch MAIN and branch.
     * @param branch1Head The current merge branch
     * @param branch2Head the other merge branch
     * @return String representing sha1 address of split point commit
     */
    private static Commit findSplit(Commit branch1Head, Commit branch2Head) {
        ArrayList<String> currCommits = new ArrayList<>();
        if (branch1Head.getId().equals(branch2Head.getId())) {
            return branch1Head;
        }
        if (branch1Head.getParent2() != null) {
            currCommits.add(branch1Head.getParent2());
        }
        for (Commit curr: branch1Head) {
            currCommits.add(curr.getId());
            if (curr.getParent2() != null) {
                currCommits.add(curr.getParent2());
            }
        }
        if (branch2Head.getParent2() != null) {
            if (currCommits.contains(branch2Head.getParent2())) {
                return Commit.read(branch2Head.getParent2());
            }

        }
        for (Commit other: branch2Head) {
            if (currCommits.contains(other.getId())) {
                return other;
            }
            if (other.getParent2() != null && currCommits.contains(other.getParent())) {
                return Commit.read(branch2Head.getParent2());
            }
        }
        throw Utils.error("No split point found", "Impossible result");
    }

}
