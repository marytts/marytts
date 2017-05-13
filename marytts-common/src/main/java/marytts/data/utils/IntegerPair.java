package marytts.data.utils;

/**
 * Encapsulation of a pair of integer for hashing purpose
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class IntegerPair {
	/** the left member of the pair */
	private Integer left;

	/** the right member of the pair */
	private Integer right; // right member of pair

	/**
	 * Constructor
	 *
	 * @param left
	 *            the left member to set
	 * @param right
	 *            the right member to set
	 */
	public IntegerPair(Integer left, Integer right) {
		this.left = left;
		this.right = right;
	}

	/**
	 * Get the left member of the pair
	 *
	 * @return the left member of the pair
	 */
	public Integer getLeft() {
		return left;
	}

	/**
	 * Get the right member of the pair
	 *
	 * @return the right member of the pair
	 */
	public Integer getRight() {
		return right;
	}

	/**
	 * Method to compare the current pair of integer to a given one
	 *
	 * @param obj
	 *            the other pair of integer
	 * @return true if obj is a pair of integer equals to the current one
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		return ((getLeft().equals(((IntegerPair) obj).getLeft()))
				&& (getRight().equals(((IntegerPair) obj).getRight())));
	}

	/**
	 * Returns a suitable hash code. The hash code follows the definition in
	 * {@code Map.Entry}.
	 *
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		// see Map.Entry API specification
		return (getLeft() == null ? 0 : getLeft().hashCode()) ^ (getRight() == null ? 0 : getRight().hashCode());
	}

	/**
	 * Method to generate a string representation of the current pair
	 *
	 * @return the string representation of the current pair
	 */
	@Override
	public String toString() {
		return "(" + getLeft().toString() + ", " + getRight().toString() + ")";
	}
}
