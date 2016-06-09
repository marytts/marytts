package marytts.data.item;

import java.util.HashSet;
import marytts.data.Sequence;

/**
 * Abstract class representing an item.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public abstract class Item
{
    protected HashSet<Sequence<? extends Item>> m_sequences_references;

    protected Item()
    {
        m_sequences_references = new HashSet<Sequence<? extends Item>>();
    }

    public boolean addSequenceReference(Sequence<? extends Item> seq)
    {
        return m_sequences_references.add(seq);
    }

    public boolean removeSequenceReference(Sequence<? extends Item> seq)
    {
        return m_sequences_references.remove(seq);
    }

    public boolean isInSequence(Sequence<? extends Item> seq)
    {
        return m_sequences_references.contains(seq);
    }
}
