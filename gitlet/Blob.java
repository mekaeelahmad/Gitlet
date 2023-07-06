package gitlet;
import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**This class is used to serialize files that are in each commit.
 * More specifically, it has access to the blob folder in the git directory
 * which holds these serialized files at different points of time.
 * @author Mekaeel Ahmad, Vinay Agrawal*/
public class Blob implements Serializable, Dumpable {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = Utils.join(CWD, ".gitlet");

    /** Blob directory. */
    public static final File BLOB_DIR = Utils.join(GITLET_DIR, "blobs");

    /** file name being stored in blob. */
    private String _name;

    /** Contents of file being stored. */
    private byte[] _contents;

    /**Constructor for Blob class.
     * @param name name of the file
     * @contents contents of the current file */
    public Blob(String name, byte[] contents) {
        _name = name;
        _contents = contents;
    }

    /**Access methods for blob instance variables. */

    /**Returns the sha1 value of this blob object. */
    private String getId() {
        return Utils.sha1(_name, _contents);
    }
    /** Returns the contents of this file. */
    public byte[] getContents() {
        return _contents;
    }

    /** Method to save Blob object in blob directory. */
    public String saveBlob() {
        String id = getId();
        File newBlob = Utils.join(BLOB_DIR, id);
        if (!newBlob.exists()) {
            Utils.writeObject(newBlob, this);
        }
        return id;
    }


    /** This method returns the contents with the specified reference id.
     * @param id id of blob object. */
    public static byte[] readBlob(String id) {
        File blobFile = Utils.join(BLOB_DIR, id);
        Blob blob = Utils.readObject(blobFile, Blob.class);
        return blob.getContents();
    }

    /** Creates new blob object and saves to blob directory if it does not already exist. */
    public static String create(String name, byte[] contents) {
        Blob newBlob = new Blob(name, contents);
        String id = newBlob.saveBlob();
        return id;
    }

    /** Static class method that checks if certain blob exists blob directory.
     * @param blob id of blob object to check
     * @return boolean indicating if blob exists
     * */
    public static boolean exists(String blob) {
        return Utils.join(BLOB_DIR, blob).exists();
    }

    @Override
    /**Used to debugging in order to see name of the file and its contents. */
    public void dump() {
        System.out.println(_name);
        System.out.println(new String(_contents, StandardCharsets.UTF_8));
    }
}
