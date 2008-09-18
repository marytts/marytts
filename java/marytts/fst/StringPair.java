package marytts.fst;

/**
 * A Pair of Strings.
 * @author benjaminroth
 */
public class StringPair{

    //private boolean hasHash;
    //private int hash;
    private String string1;
    private String string2;

    public StringPair(String s1, String s2) {
        this.string1 = s1;
        this.string2 = s2;
        //this.hasHash = false;
    }

    public void setString1(String s1){
        this.string1 = s1;
    }

    public void setString2(String s2){
        this.string2 = s2;
    }

    public int hashCode() {
        /*if (!hasHash){
            this.hash = 31 * string1.hashCode() + string2.hashCode();
            this.hasHash = true;
        }

        return this.hash;*/
        return 31 * string1.hashCode() + string2.hashCode();
    }

    public boolean equals(Object o) {

        if (o instanceof StringPair &&
                ((StringPair) o).getString1().equals(string1) &&
                ((StringPair) o).getString2().equals(string2))
                return true;

        return false;
    }

    public String getString1() {
        return string1;
    }

    public String getString2() {
        return string2;
    }
    
    public String toString(){
        return string1 + " " + string2;
    }
}

