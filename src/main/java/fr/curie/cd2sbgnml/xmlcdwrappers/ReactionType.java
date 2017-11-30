package fr.curie.cd2sbgnml.xmlcdwrappers;

/**
 * Possible values for the reactionType element of CellDesigner reactions.
 */
public enum ReactionType {

    STATE_TRANSITION, HETERODIMER_ASSOCIATION, DISSOCIATION,

    KNOWN_TRANSITION_OMITTED, UNKNOWN_TRANSITION,
    CATALYSIS, UNKNOWN_CATALYSIS,
    INHIBITION, UNKNOWN_INHIBITION,
    TRANSPORT,
    TRANSCRIPTIONAL_ACTIVATION, TRANSCRIPTIONAL_INHIBITION,
    TRANSLATIONAL_ACTIVATION, TRANSLATIONAL_INHIBITION,
    PHYSICAL_STIMULATION, MODULATION, TRIGGER, REDUCED_MODULATION,
    REDUCED_TRIGGER, NEGATIVE_INFLUENCE, POSITIVE_INFLUENCE,

    /**
     * When the reaction involves only a logic gate, pointing directly at en entity, with no process involved.
     */
    BOOLEAN_LOGIC_GATE
}
