package marytts.data;

import marytts.data.item.Item;

import java.util.Hashtable;
import org.apache.commons.lang3.tuple.ImmutablePair;

import org.jgrapht.*;
import org.jgrapht.generate.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class RelationGraph
{

    private Hashtable<ImmutablePair<SupportedSequenceType, SupportedSequenceType>, Relation> m_relations;
    private Graph<Sequence, DefaultEdge> actual_graph;

    public RelationGraph()
    {
        actual_graph = new SimpleGraph<>(DefaultEdge.class);
        m_relations = new Hashtable<ImmutablePair<SupportedSequenceType, SupportedSequenceType>, Relation>();
    }

    public void addRelation(SupportedSequenceType source, SupportedSequenceType target, Relation rel)
    {
        getRelations().put(new ImmutablePair<SupportedSequenceType, SupportedSequenceType>(source, target), rel);
    }


    public void removeRelation(Relation rel)
    {
    }


    public void removeRelation(Sequence<? extends Item> source,
                               Sequence<? extends Item> target)
    {
    }

    public Relation getRelation(SupportedSequenceType source, SupportedSequenceType target)
    {
        return getRelations().get(new ImmutablePair<SupportedSequenceType, SupportedSequenceType>(source, target));
    }

    public Hashtable<ImmutablePair<SupportedSequenceType, SupportedSequenceType>, Relation> getRelations()
    {
        return m_relations;
    }
}
