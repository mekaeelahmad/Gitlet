package gitlet;


import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.plainFilenamesIn;

/** Represents a gitlet commit object.
 *  This commit class stores each commit with its respective seralized files.
 *  Additionally, this commit class contains the two parents of a commit in
 *  order to easily traverse through to past commits.
 *
 *  @author Mekaeel Ahmad, Vinay Agrawal
 */
public class Commit implements Serializable, Iterable<Commit>, Dumpable {
    /** This is the current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The gitlet directory. */
    public static final File GITLET_DIR = Utils.join(CWD, ".gitlet");

    /** Reference to Head commit. */
    public static final File HEAD = Utils.join(GITLET_DIR, "HEAD");

    /** Commit Directory. */
    public static final File COMMIT_DIR = Utils.join(GITLET_DIR, "commits");

    /** Date Format. */
    public static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

    /** The message of this Commit. */
    private String _message;

    /** Time of Intialization. */
    private String _time;

    /** Reference to Parent1 Commit Id. */
    private String _parent1;

    /** Reference to Parent 2 Commit Id. */
    private String _parent2;

    /** List of Object references. */
    private HashMap<String, String> _blobs;

    /** Commit Constructor.
     * @param blobs HashMap that stores the fileName as key
     * and value as sha1 for its object.
     * @param msg Commit message.
     * @param prt ID of parent commit. */
    public Commit(String msg, String prt, HashMap<String, String> blobs) {
        FORMAT.setTimeZone(TimeZone.getTimeZone("PST"));
        Date time = new Date();
        _time = FORMAT.format(time);
        _message = msg;
        _parent1 = prt;
        _parent2 = null;
        _blobs = blobs;
    }

    /** Iterator method. Allows user to traverse through all commits until the initial
     * commit. Does not include traversing through second parents.*/
    public Iterator<Commit> iterator() {
        return new CommitIterator();
    }

    /** Used to debug blob HashMap. */
    @Override
    public void dump() {
        System.out.println(this);
        Set<String> keys = _blobs.keySet();
        for (String c: keys) {
            System.out.println(c);
        }
    }

    /** Traverses from current commit to initial commit and returns new commit everytime
    next is called. This class does not account for second parents. */
    private class CommitIterator implements Iterator<Commit> {

        /** First value that is returned is this commit. */
        private String head = getId();

        /** Returns the next parent commit. */
        public Commit next() {
            Commit elem = Commit.read(head);
            head = elem.getParent();
            return elem;
        }

        /** Returns whether of not there is a next commit. */
        public boolean hasNext() {
            return head != null;
        }
    }

    /** Returns this Commits message. */
    public String getMessage() {
        return _message;
    }

    /** Returns the sha1 value for this commits parent. */
    public String getParent() {
        return _parent1;
    }

    /**Returns the sha1 value for this commits second parent if it has one. */
    public String getParent2() {
        return _parent2;
    }

    /**Returns the sha1 value for this commit. */
    public String getId() {
        String blobHolder = "";
        String parentHolder = "";
        if (_blobs != null) {
            blobHolder = _blobs.toString();
        }
        if (_parent1 != null) {
            parentHolder = _parent1;
        }
        return Utils.sha1(_message, _time, parentHolder, blobHolder);
    }

    /** Returns a HashMap containing file names as keys and sha1 ID's for blobs as values. */
    public HashMap<String, String> getBlobs() {
        return _blobs;
    }

    /** Returns the sha1 value of the files object if the file is in this commit.
     * @param file filename for file you're looking for. */
    public String getFile(String file) {
        String blobId = null;
        if (_blobs != null && _blobs.containsKey(file)) {
            blobId = _blobs.get(file);
        }
        return blobId;
    }

    /** Utility method to addParent after merge.
     * @param parent2 sha1 value of second parent.*/
    public void addParent(String parent2) {
        _parent2 = parent2;
        return;
    }

    /** String representation of commit. */
    @Override
    public String toString() {
        String merge = "";
        if (_parent2 != null) {
            merge = "Merge: " + _parent1.substring(0, 7) + " " + _parent2.substring(0, 7) + "\n";
        }
        String res = "===" + "\n" + "commit " + getId() + "\n";
        res += merge;
        res += "Date: " + _time.toString() + "\n";
        res += _message + "\n";
        return res;
    }

    /** Saves commit object in commit directory. */
    private String saveCommit() {
        String commitId = getId();
        File newCommit = Utils.join(COMMIT_DIR, commitId);
        Utils.writeObject(newCommit, this);
        return commitId;
    }

    /** Checks if specified file is in commit object.
     * @param file name of file you're looking for. */
    public boolean tracks(String file) {
        return _blobs != null && getFile(file) != null;
    }

    /** Returns the commit object referenced by the sha1 value if it exists.
     * @param id sha1 value of the commit. */
    public static Commit read(String id) {
        Commit commit = null;
        for (String name: plainFilenamesIn(COMMIT_DIR)) {
            if (name.startsWith(id)) {
                id = name;
            }
        }
        File commitFile = Utils.join(COMMIT_DIR, id);
        if (commitFile.exists()) {
            commit = Utils.readObject(commitFile, Commit.class);
        }
        return commit;
    }

    /** Creates a new commit and adds it to the COMMIT Directory. Returns sha1 reference of commit.
     * @param msg commit message.
     * @param p2 sha1 value of parent2.
     * @param p1 sha1 value of parent1.
     * @param blobs new HashMap representing files in this commit and their serialized objects. */
    public static String create(String msg, String p1, String p2, HashMap<String, String> blobs) {
        Commit newCommit = new Commit(msg, p1, blobs);
        newCommit.addParent(p2);
        String id = newCommit.saveCommit();
        return id;
    }


    /** Returns boolean indicating if commit exists or not.
     * @param id sha1 value of the commit*/
    public static boolean exists(String id) {
        return Utils.join(COMMIT_DIR, id).exists();
    }

    /** Returns true if file is tracked in given commit and false otherwise.
     * @param file name of the file you're looking for in the commit*/
    public boolean isTracked(String file) {
        return _blobs != null && _blobs.containsKey(file);
    }

    /** Returns true if file contents are different and false otherwise.
     * @param other represents other commit
     * @param file name of file you're looking for*/
    public boolean isDifferent(Commit other, String file) {
        if (other.getFile(file) == null && getFile(file) == null) {
            return false;
        } else if (other.getFile(file) == null || getFile(file) == null) {
            return true;
        }
        return !other.getFile(file).equals(getFile(file));
    }

    /** Returns a list of names that this commit has saved. */
    public Set<String> getTracked() {
        return _blobs.keySet();
    }

    /** Combines all tracked files into a set. No repeated file names because of HashSet.
     * @param commits multiple commits */
    public static Set<String> concatenateFiles(Commit... commits) {
        if (commits.length == 0) {
            return null;
        }
        HashSet<String> files = new HashSet<>();
        for (int i = 0; i < commits.length; i++) {
            Set<String> filesInCommit = commits[i].getTracked();
            for (String f: filesInCommit) {
                files.add(f);
            }
        }
        return files;
    }
}

