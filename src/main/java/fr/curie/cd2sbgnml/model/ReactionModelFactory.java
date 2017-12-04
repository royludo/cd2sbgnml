package fr.curie.cd2sbgnml.model;

import fr.curie.cd2sbgnml.xmlcdwrappers.ReactionWrapper;

import static fr.curie.cd2sbgnml.xmlcdwrappers.ReactionType.DISSOCIATION;
import static fr.curie.cd2sbgnml.xmlcdwrappers.ReactionType.HETERODIMER_ASSOCIATION;
import static fr.curie.cd2sbgnml.xmlcdwrappers.ReactionType.TRUNCATION;

/**
 * A simple factory that creates the right reaction model depending on the reaction type.
 */
public class ReactionModelFactory {
    public static GenericReactionModel create(ReactionWrapper reactionWrapper) {
        if(reactionWrapper.getReactionType() == HETERODIMER_ASSOCIATION) {
            return new AssociationReactionModel(reactionWrapper);
        }
        else if(reactionWrapper.getReactionType() == DISSOCIATION) {
            return new DissociationReactionModel(reactionWrapper);
        }
        else if(reactionWrapper.getReactionType() == TRUNCATION) {
            return new DissociationReactionModel(reactionWrapper);
        }
        else {
            return new SimpleReactionModel(reactionWrapper);
        }

    }
}
