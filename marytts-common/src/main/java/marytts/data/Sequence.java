package marytts.data;

import java.util.ArrayList;
import java.util.HashSet;
import marytts.data.item.Item;

/**
 * A sequence is just an array list of uniform type elements
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class Sequence<E extends Item> extends ArrayList<E>
{
    private static int id_cpt = 0;
    protected int id;
    protected HashSet<Relation> m_source_relation_references;
    protected HashSet<Relation> m_target_relation_references;

    public Sequence()
    {
        super();
        id = id_cpt;
        id_cpt++;
        m_source_relation_references = new HashSet<Relation>();
        m_target_relation_references = new HashSet<Relation>();
    }

    /**********************************************************************
     ** Relation part
     **********************************************************************/
    public boolean addSourceRelationReference(Relation rel)
    {
        return m_source_relation_references.add(rel);
    }

    public boolean addTargetRelationReference(Relation rel)
    {
        return m_target_relation_references.add(rel);
    }

    public boolean deleteRelationReference(Relation rel)
    {
        if (m_source_relation_references.contains(rel))
            return m_source_relation_references.remove(rel);

        return m_target_relation_references.remove(rel);
    }

    public Relation getRelationAsSource(Sequence<E> other)
    {
        Relation found_relation = null;
        // Relations when the current sequence is the source
        for (Relation rel: m_source_relation_references)
        {
            if (rel.getTarget().equals(other))
            {
                found_relation = rel;
                break;
            }
        }

        return found_relation;
    }

    public Relation getRelationAsTarget(Sequence<E> other)
    {
        Relation found_relation = null;
        // Relations when the current sequence is the target
        for (Relation rel: m_target_relation_references)
        {
            if (rel.getSource().equals(other))
            {
                found_relation = rel;
                break;
            }
        }

        return found_relation;
    }

    public boolean isRelatedWith(Sequence<E> other)
    {
        return (getRelationAsSource(other) != null) || (getRelationAsTarget(other) != null);
    }
    /**********************************************************************
     ** Override standard methodologies to support back-references
     **********************************************************************/
    @Override
    public boolean add(E it)
    {
        it.addSequenceReference(this);
        return super.add(it);
    }

    @Override
    public E remove(int index)
    {
        E it = super.remove(index);
        it.removeSequenceReference(this);
        return it;
    }

    @Override
    public boolean remove(Object it)
    {
        ((Item) it).removeSequenceReference(this);
        return super.remove(it);
    }

    @Override
    public int hashCode()
    {
        return id;
    }
}
