// This is a super basic store class. For our purposes, we only need an id.
// For full implementation, this would have much more information, such as database connections, location, employees, etc.

public class StoreNode {

    private final String id;
    
    /**
     * Constructor
     * @param id the store ID, a string representing the unique identifier of the store
     */
    public StoreNode(String id) {
        this.id = id;
    }

   public String getId() {
    return id;
   }

}// end class StoreNode