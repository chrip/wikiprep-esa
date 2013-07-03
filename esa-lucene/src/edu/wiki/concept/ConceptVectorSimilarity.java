package edu.wiki.concept;

import edu.wiki.api.concept.IConceptIterator;
import edu.wiki.api.concept.IConceptVector;
import edu.wiki.api.concept.search.IScorer;



public class ConceptVectorSimilarity {

	IScorer mScorer;
	boolean isESA = true;

	public ConceptVectorSimilarity( IScorer easScorer, IScorer gesaScorer  ) {
		if (isESA) {
			// ESA
			mScorer = easScorer;
		}
		else {
			// GeSA
			mScorer = gesaScorer;
		}
	}

	public double calcSimilarity( IConceptVector v0, IConceptVector v1 ) {
		
		if (isESA) {
			// ESA
			// cosine similarity
			mScorer.reset( v0.getData(), v1.getData(), 1 );
			IConceptIterator it0 = v0.iterator();
			while( it0.next() ) {
				double value1 = v1.get( it0.getId() );
				if( value1 > 0 ) {
					mScorer.addConcept( it0.getId(), it0.getValue(), it0.getId(), value1, 1 );
				}
			}
		}
		else {
			// GeSA
			mScorer.reset( v0.getData(), v1.getData(), 1 );
			IConceptIterator it0 = v0.iterator();
			it0 = v0.orderedIterator();
			while( it0.next() ) {
				mScorer.addConcept( it0.getId(), it0.getValue(), 0, 0, 1 );			
			}
			IConceptIterator it1 = v1.orderedIterator();
			while( it1.next() ) {
				mScorer.addConcept( 0, 0, it1.getId(), it1.getValue(), 1 );			
			}
		}
		mScorer.finalizeScore( v0.getData(), v1.getData() );
		return mScorer.getScore();
	}

}
