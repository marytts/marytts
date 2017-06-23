package marytts.data;

import java.util.ArrayList;
import java.util.HashSet;
import marytts.data.item.Item;

/**
 * A sequence is just an array list of uniform type elements
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class Sequence<E extends Item> extends ArrayList<E> {
    /** The counter to generate identifier */
    private static int id_cpt = 0;

    /** The sequence identifier */
    protected int id;

    /** The set of relations in which the sequence is the source */
    protected HashSet<Relation> m_source_relation_references;

    /** The set of relations in which the sequence is the target */
    protected HashSet<Relation> m_target_relation_references;

    /**
     * The constructor. A sequence is empty at the beginning
     */
    public Sequence() {
        super();
        id = id_cpt;
        id_cpt++;
        m_source_relation_references = new HashSet<Relation>();
        m_target_relation_references = new HashSet<Relation>();
    }

    /**********************************************************************
     ** Relation part
     **********************************************************************/
    /**
     * Adding a relation in which the sequence is the source
     *
     * @param rel
     *            the relation in which the sequence is the source
     * @return true if the relation was not already added
     */
    public boolean addSourceRelationReference(Relation rel) {
        return m_source_relation_references.add(rel);
    }

    /**
     * Adding a relation in which the sequence is the target
     *
     * @param rel
     *            the relation in which the sequence is the target
     * @return true if the relation was not already added
     */
    public boolean addTargetRelationReference(Relation rel) {
        return m_target_relation_references.add(rel);
    }

    /**
     * Delete the relation from the source or target set
     *
     * @param rel
     *            the relation to delete
     * @return true if the target or the source sets contained the specified
     *         relation
     */
    public boolean deleteRelationReference(Relation rel) {
        if (m_source_relation_references.contains(rel)) {
            return m_source_relation_references.remove(rel);
        }

        return m_target_relation_references.remove(rel);
    }

    /**
     * Get the relation between the current sequence (which is the source) and a
     * given sequence
     *
     * @param other
     *            the other sequence
     * @return the relation if found, null else
     */
    public Relation getRelationAsSource(Sequence<E> other) {
        Relation found_relation = null;
        // Relations when the current sequence is the source
        for (Relation rel : m_source_relation_references) {
            if (rel.getTarget().equals(other)) {
                found_relation = rel;
                break;
            }
        }

        return found_relation;
    }

    /**
     * Get the relation between a given sequence and the current sequence (which
     * is the target)
     *
     * @param other
     *            the other sequence
     * @return the relation if found, null else
     */
    public Relation getRelationAsTarget(Sequence<E> other) {
        Relation found_relation = null;

        // Relations when the current sequence is the target
        for (Relation rel : m_target_relation_references) {
            if (rel.getSource().equals(other)) {
                found_relation = rel;
                break;
            }
        }

        return found_relation;
    }

    /**
     * Method to know if the current sequence is related to a given sequence
     *
     * @param other
     *            the given sequence
     * @return true if there is an existing relation, false else
     */
    public boolean isRelatedWith(Sequence<E> other) {
        return (getRelationAsSource(other) != null) || (getRelationAsTarget(other) != null);
    }

    /**********************************************************************
     ** Override ArrayList standard methods to support back-references
     **********************************************************************/
    /**
     * Appends the specified element to the end of the sequence
     *
     * @param e
     *            element to be appended to this sequence
     * @return true (as specified by Collection.add(E))
     */
    @Override
    public boolean add(E e) {
        return add(e, false);
    }

    /**
     * Inserts the specified element at the specified position in this sequence.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * @param index
     *            index at which the specified element is to be inserted
     * @param e
     *            element to be inserted to this sequence
     */
    @Override
    public void add(int index, E e) {
        add(index, e, false);
    }

    /**
     * Appends the specified element to the end of the sequence and maybe expand
     * the relations in which the sequence is involved.
     *
     * @param e
     *            element to be appended to this sequence
     * @param expand_relation
     *            true leads to expand the relations, false is not touching the
     *            relations
     * @return true (as specified by Collection.add(E))
     */
    public boolean add(E e, boolean expand_relation) {
        add(size() - 1, e, expand_relation);

        return true;
    }

    /**
     * Inserts the specified element at the specified position in this sequence.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).maybe expand the
     * relations in which the sequence is involved.
     *
     * @param index
     *            index at which the specified element is to be inserted
     * @param expand_relation
     *            true leads to expand the relations, false is not touching the
     *            relations
     * @param e
     *            element to be inserted to this sequence
     */
    public void add(int index, E e, boolean expand_relation) {
        super.add(e);
        e.setSequenceReference(this);

        // Remove the items from the target relations
        for (Relation rel : m_target_relation_references) {
            rel.addTargetItem(index, expand_relation);
        }

        // Remove the items from the source relations
        for (Relation rel : m_source_relation_references) {
            rel.addSourceItem(index, expand_relation);
        }

    }

    /**
     * Replaces the element at the specified position in this sequence with the
     * specified element.
     *
     * @param index
     *            index of the element to replace
     * @param element
     *            element to be stored at the specified position
     * @return the element previously at the specified position
     */
    @Override
    public E set(int index, E element) {
        E previous = super.set(index, element);

        element.setSequenceReference(this);

        previous.unsetSequenceReference();

        return previous;
    }

    /**
     * Removes the element at the specified position in this sequence. Shifts
     * any subsequent elements to the left (subtracts one from their indices).
     *
     * @param index
     *            the index of the element to be removed
     * @return the element that was removed from the sequence
     */
    @Override
    public E remove(int index) {
        // Remove the items from the target relations
        for (Relation rel : m_target_relation_references) {
            rel.removeTargetItem(index);
        }

        // Remove the items from the source relations
        for (Relation rel : m_source_relation_references) {
            rel.removeSourceItem(index);
        }

        // Finally remove the item from the sequence
        E it = super.remove(index);

        // Update the reference of the item to indicate that it is not a member
        // of the sequence
        it.unsetSequenceReference();

        return it;
    }

    /**
     * Removes the first occurrence of the specified element from this sequence,
     * if it is present. If the sequence does not contain the element, it is
     * unchanged. More formally, removes the element with the lowest index i
     * such that (o==null ? get(i)==null : o.equals(get(i))) (if such an element
     * exists). Returns true if this sequence contained the specified element
     * (or equivalently, if this sequence changed as a result of the call).
     *
     * @param o
     *            element to be removed from this sequence, if present
     * @return true if this sequence contained the specified element
     */
    @Override
    public boolean remove(Object o) {
        int idx = this.indexOf(o);
        if (idx < 0) {
            return false;
        }

        // Clean references
        ((Item) o).unsetSequenceReference();

        // And remove properly the item now
        remove(idx);

        return true;
    }

    /**********************************************************************
     ** Object overriding
     **********************************************************************/
    /**
     * Method to generate a hash for the sequence. The hash is actually the id
     * (see the id property) of the sequence
     *
     * @return a hash representation of the utterance
     */
    @Override
    public int hashCode() {
        return id;
    }

    /**
     * Method to compare an object to the current sequence
     *
     * @param obj
     *            the object to compare
     * @return true if obj is a sequence and the hashcode are equals, false else
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        return (hashCode() == obj.hashCode());
    }

    /**
     * Method to generate a string representation of the current sequence
     *
     * @return the string representation of the current sequence
     */
    @Override
    public String toString() {
        if (this.size() > 0) {
            return "Seq(" + hashCode() + "," + get(0).getClass().getSimpleName() + ")";
        } else {
            return "Seq(" + hashCode() + ")";
        }
    }
}
