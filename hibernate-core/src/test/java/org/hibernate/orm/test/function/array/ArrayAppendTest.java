/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.function.array;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Expression;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = EntityWithArrays.class)
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsStructuralArrays.class)
// Make sure this stuff runs on a dedicated connection pool,
// otherwise we might run into ORA-21700: object does not exist or is marked for delete
// because the JDBC connection or database session caches something that should have been invalidated
@ServiceRegistry(settings = @Setting(name = AvailableSettings.CONNECTION_PROVIDER, value = ""))
public class ArrayAppendTest {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new EntityWithArrays( 1L, new String[]{} ) );
			em.persist( new EntityWithArrays( 2L, new String[]{ "abc", null, "def" } ) );
			em.persist( new EntityWithArrays( 3L, null ) );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.createMutationQuery( "delete from EntityWithArrays" ).executeUpdate();
		} );
	}

	@Test
	public void testAppend(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-append-example[]
			List<Tuple> results = em.createQuery( "select e.id, array_append(e.theArray, 'xyz') from EntityWithArrays e order by e.id", Tuple.class )
					.getResultList();
			//end::hql-array-append-example[]
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[]{ "xyz" }, results.get( 0 ).get( 1, String[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new String[]{ "abc", null, "def", "xyz" }, results.get( 1 ).get( 1, String[].class ) );
			assertEquals( 3L, results.get( 2 ).get( 0 ) );
			assertNull( results.get( 2 ).get( 1, String[].class ) );
		} );
	}

	@Test
	public void testAppendNull(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Tuple> results = em.createQuery( "select e.id, array_append(e.theArray, null) from EntityWithArrays e order by e.id", Tuple.class )
					.getResultList();
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[]{ null }, results.get( 0 ).get( 1, String[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new String[]{ "abc", null, "def", null }, results.get( 1 ).get( 1, String[].class ) );
			assertEquals( 3L, results.get( 2 ).get( 0 ) );
			assertNull( results.get( 2 ).get( 1, String[].class ) );
		} );
	}

	@Test
	public void testNodeBuilderArray(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
			final JpaRoot<EntityWithArrays> root = cq.from( EntityWithArrays.class );
			cq.multiselect(
					root.get( "id" ),
					cb.arrayAppend( root.<String[]>get( "theArray" ), cb.literal( "xyz" ) ),
					cb.arrayAppend( root.get( "theArray" ), "xyz" )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.arrayAppend( root.<Integer[]>get( "theArray" ), cb.literal( "xyz" ) );
//			cb.arrayAppend( root.<Integer[]>get( "theArray" ), "xyz" );
		} );
	}

	@Test
	public void testNodeBuilderCollection(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
			final JpaRoot<EntityWithArrays> root = cq.from( EntityWithArrays.class );
			cq.multiselect(
					root.get( "id" ),
					cb.collectionAppend( root.<Collection<String>>get( "theCollection" ), cb.literal( "xyz" ) ),
					cb.collectionAppend( root.get( "theCollection" ), "xyz" )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.collectionAppend( root.<Collection<Integer>>get( "theCollection" ), cb.literal( "xyz" ) );
//			cb.collectionAppend( root.<Collection<Integer>>get( "theCollection" ), "xyz" );
		} );
	}

}