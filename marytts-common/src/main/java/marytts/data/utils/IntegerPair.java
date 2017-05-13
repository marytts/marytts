package marytts.data.utils;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class IntegerPair {
	private Integer left; // left member of pair
	private Integer right; // right member of pair

	public IntegerPair(Integer left, Integer right) {
		this.left = left;
		this.right = right;
	}

	public Integer getLeft() {
		return left;
	}

	public Integer getRight() {
		return right;
	}

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
	 * <p>
	 * Returns a suitable hash code. The hash code follows the definition in
	 * {@code Map.Entry}.
	 * </p>
	 *
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		// see Map.Entry API specification
		return (getLeft() == null ? 0 : getLeft().hashCode()) ^ (getRight() == null ? 0 : getRight().hashCode());
	}

	@Override
	public String toString() {
		return "(" + getLeft().toString() + ", " + getRight().toString() + ")";
	}
}
