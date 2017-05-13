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
 * Graph of the relations. It is separating clearly the available relations from
 * the one which are calculated. This class should not used directly. The
 * Utterance class is the entry point.
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class RelationGraph {

	/**
	 * A helper class to represent a include a relation to an edge for the graph
	 * representation.
	 *
	 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
	 *         Maguer</a>
	 */
	public static class RelationEdge<V> extends DefaultEdge {
		/** The source node */
		private V m_source;

		/** The target node */
		private V m_target;

		/** The relation as the value of the edge */
		private Relation m_relation;

		/**
		 * The constructor
		 *
		 * @param source
		 *            the source node
		 * @param target
		 *            the target node
		 * @param relation
		 *            the relation as the value of the edge
		 */
		public RelationEdge(V source, V target, Relation relation) {
			this.m_source = source;
			this.m_target = target;
			this.m_relation = relation;
		}

		/**
		 * Method to get the source node
		 *
		 * @return the source node
		 */
		public V getSource() {
			return m_source;
		}

		/**
		 * Method to get the target node
		 *
		 * @return the target node
		 */
		public V getTarget() {
			return m_target;
		}

		/**
		 * Method to get the value: the relation
		 *
		 * @return the relation
		 */
		public Relation getRelation() {
			return m_relation;
		}

		/**
		 * Method to represent the edge in form a string. It just return the
		 * relation in a String format. The relation contains already all the
		 * needed informations.
		 *
		 * @return the string representation of the edge
		 */
		@Override
		public String toString() {
			return m_relation.toString();
		}
	}

	/** The stored relations */
	private Hashtable<ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>, Relation> m_relations;

	/** The cache for the computed relations */
	private Hashtable<ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>, Relation> m_computed_relations;

	/**
	 * The relation graph whose nodes are the sequences and edge's values the
	 * relations
	 */
	private Graph<Sequence<? extends Item>, RelationEdge> m_actual_graph;

	/**
	 * Constructor of the relation graph. At te beginning it is empty.
	 */
	public RelationGraph() {
		m_actual_graph = new SimpleGraph<>(RelationEdge.class);
		m_relations = new Hashtable<ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>, Relation>();
		m_computed_relations = new Hashtable<ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>, Relation>();
	}

	/**
	 * Method to add a relation to the graph.
	 *
	 * @param rel
	 *            the relation to add
	 */
	public void addRelation(Relation rel) {
		// Add the relation to the map
		getRelations().put(
				new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(rel.getSource(), rel.getTarget()),
				rel);

		// Creating the edge
		m_actual_graph.addVertex(rel.getSource());
		m_actual_graph.addVertex(rel.getTarget());
		m_actual_graph.addEdge(rel.getSource(), rel.getTarget(),
				new RelationEdge(rel.getSource(), rel.getTarget(), rel));
	}

	/**
	 * Method to remove a relation
	 *
	 * @param rel
	 *            the relation to remove
	 */
	public void removeRelation(Relation rel) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Method to remove a relation given the sequences identifying the relation
	 *
	 * @param source
	 *            the source sequence of the relation
	 * @param target
	 *            the target sequence of the relation
	 */
	public void removeRelation(Sequence<? extends Item> source, Sequence<? extends Item> target) {
		Relation rel = getRelation(source, target);
		if (rel != null)
			removeRelation(rel);
	}

	/**
	 * Method to get the sequence given the sequences identifying the relation.
	 * This relation might be computed if it is not existing on the graph.
	 *
	 * @param source
	 *            the source sequence of the relation
	 * @param target
	 *            the target sequence of the relation
	 * @return the found or computed relation, null if there is no way in the
	 *         graph to compute this relation.
	 */
	public Relation getRelation(Sequence<? extends Item> source, Sequence<? extends Item> target) {
		// check if source or target are present in the graph
		if (!m_actual_graph.containsVertex(source)) {
			return null;
		}

		if (!m_actual_graph.containsVertex(target)) {
			return null;
		}

		// Try to get the direct relation
		Relation final_rel = getRelations()
				.get(new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(source, target));

		// Relation found
		if (final_rel != null)
			return final_rel;

		// Try the precomputedrelations
		final_rel = getComputedRelations()
				.get(new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(source, target));

		// Relation found
		if (final_rel != null)
			return final_rel;

		// Try to build the relation
		synchronized (m_actual_graph) {
			List<RelationEdge> list_edges = (new DijkstraShortestPath(m_actual_graph, source, target))
					.getPathEdgeList();

			// No path found
			if (list_edges.isEmpty()) {
				getComputedRelations().put(
						new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(source, target), null);
				return null;
			}

			// Initialisation
			RelationEdge first = list_edges.remove(0);
			Sequence<? extends Item> cur_source = (Sequence<? extends Item>) first.getSource();
			Sequence<? extends Item> cur_target = (Sequence<? extends Item>) first.getTarget();
			if (cur_source == source)
				final_rel = getRelations().get(
						new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(cur_source, cur_target));
			else {
				final_rel = getRelations().get(
						new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(cur_source, cur_target))
						.getReverse();
			}
			Sequence<? extends Item> prev_target = cur_target;

			for (RelationEdge cur_edge : list_edges) {
				cur_source = (Sequence<? extends Item>) cur_edge.getSource();
				cur_target = (Sequence<? extends Item>) cur_edge.getTarget();

				Relation cur_rel = getRelations().get(
						new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(cur_source, cur_target));
				if (cur_source != prev_target) {
					cur_rel = getRelations()
							.get(new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(cur_source,
									cur_target))
							.getReverse();
				}
				final_rel = final_rel.compose(cur_rel);

				prev_target = cur_target;
			}
		}

		// save the computed relation into the cache of precomputed relations
		getComputedRelations()
				.put(new ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>(source, target), final_rel);

		return final_rel;
	}

	/**
	 * Method to get the stored relations
	 *
	 * @return the stored relations
	 */
	public Hashtable<ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>, Relation> getRelations() {
		return m_relations;
	}

	/**
	 * Method to get the computed relations
	 *
	 * @return the computed relations
	 */
	public Hashtable<ImmutablePair<Sequence<? extends Item>, Sequence<? extends Item>>, Relation> getComputedRelations() {
		return m_computed_relations;
	}
}
