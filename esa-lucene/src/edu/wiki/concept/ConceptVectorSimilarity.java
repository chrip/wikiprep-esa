package edu.wiki.concept;

import edu.wiki.api.concept.IConceptIterator;
import edu.wiki.api.concept.IConceptVector;
import edu.wiki.api.concept.search.IScorer;


public class ConceptVectorSimilarity {

	IScorer mEsaScorer;
	IScorer mGesaScorer;
	
	public ConceptVectorSimilarity( IScorer easScorer, IScorer gesaScorer  ) {
		mEsaScorer = easScorer;
		mGesaScorer = gesaScorer;
	}
	
	public double calcSimilarity( IConceptVector v0, IConceptVector v1 ) {
//		mEsaScorer.reset( v0.getData(), v1.getData(), 1 );
		mGesaScorer.reset( v0.getData(), v1.getData(), 1 );
		
		IConceptIterator it0 = v0.iterator();
//		while( it0.next() ) {
//			double value1 = v1.get( it0.getId() );
//			if( value1 > 0 ) {
//				mEsaScorer.addConcept( it0.getId(), it0.getValue(), it0.getId(), value1, 1 );
//			}
//		}
		it0 = v0.orderedIterator();
		while( it0.next() ) {
			mGesaScorer.addConcept( it0.getId(), it0.getValue(), 0, 0, 1 );			
		}
		IConceptIterator it1 = v1.orderedIterator();
		while( it1.next() ) {
			mGesaScorer.addConcept( 0, 0, it1.getId(), it1.getValue(), 1 );			
		}
		
		//		mEsaScorer.finalizeScore( v0.getData(), v1.getData() );
		mGesaScorer.finalizeScore( v0.getData(), v1.getData() );
		
		//
		return mGesaScorer.getScore();
		//return mEsaScorer.getScore();
	}
	
}
