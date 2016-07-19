package marytts.data;

import marytts.data.item.Item;
import java.util.List;
import java.util.Hashtable;
import org.apache.commons.lang3.tuple.ImmutablePair;

import org.jgrapht.*;
import org.jgrapht.alg.DijkstraShortestPath;
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
    public static class RelationEdge<V> extends DefaultEdge {
        private V m_source;
        private V m_target;
        private Relation m_relation;

        public RelationEdge(V source, V target, Relation relation)
        {
            this.m_source = source;
            this.m_target = target;
            this.m_relation = relation;
        }

        public V getSource()
        {
            return m_source;
        }

        public V getTarget()
        {
            return m_target;
        }

        public Relation getRelation()
        {
            return m_relation;
        }

        public String toString()
        {
            return m_relation.toString();
        }
    }


    private Hashtable<ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>, Relation> m_relations;
    private Graph<Sequence<? extends Item>, RelationEdge> m_actual_graph;

    public RelationGraph()
    {
        m_actual_graph = new SimpleGraph<>(RelationEdge.class);
        m_relations = new Hashtable<ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>, Relation>();
    }

    public void addRelation(Relation rel)
    {
        // Add the relation to the map
        getRelations().put(new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(rel.getSource(), rel.getTarget()), rel);

        // Creating the edge
        m_actual_graph.addVertex(rel.getSource());
        m_actual_graph.addVertex(rel.getTarget());
        m_actual_graph.addEdge(rel.getSource(), rel.getTarget(),
                               new RelationEdge(rel.getSource(),
                                                rel.getTarget(),
                                                rel));
    }


    public void removeRelation(Relation rel)
    {
    }


    public void removeRelation(Sequence<? extends Item> source,
                               Sequence<? extends Item> target)
    {
    }

    public Relation getRelation(Sequence<? extends Item> source,
                                Sequence<? extends Item> target)
    {

        // check if source or target are present in the graph
        if (!m_actual_graph.containsVertex(source))
            return null;

        if (!m_actual_graph.containsVertex(target))
            return null;

        // Try to get the direct relation
        Relation final_rel = getRelations().get(new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(source, target));

        // Relation found
        if (final_rel != null)
            return final_rel;


        // Try to build the relation
        synchronized(m_actual_graph)
        {
            List<RelationEdge> list_edges = (new DijkstraShortestPath(m_actual_graph, source, target)).getPathEdgeList();

            // No path found
            if (list_edges.isEmpty())
                return null;

            // Initialisation
            RelationEdge first = list_edges.remove(0);
            Sequence<? extends Item> cur_source = (Sequence<? extends Item>) first.getSource();
            Sequence<? extends Item> cur_target = (Sequence<? extends Item>) first.getTarget();
            final_rel = getRelations().get(new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(cur_source, cur_target));
            if (final_rel == null)
                final_rel = getRelations().get(new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(cur_target, cur_source)).getReverse();

            for (RelationEdge cur_edge: list_edges)
            {
                cur_source = (Sequence<? extends Item>) cur_edge.getSource();
                cur_target = (Sequence<? extends Item>) cur_edge.getTarget();

                Relation cur_rel = getRelations().get(new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(cur_source, cur_target));
                if (cur_rel == null)
                    cur_rel = getRelations().get(new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(cur_target, cur_source)).getReverse();

                final_rel = final_rel.compose(cur_rel);
            }
        }
        return final_rel;
    }

    public Hashtable<ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>, Relation> getRelations()
    {
        return m_relations;
    }
}
