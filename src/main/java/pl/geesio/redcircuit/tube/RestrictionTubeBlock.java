package pl.geesio.redcircuit.tube;

/**
 * Rura restrykcyjna: działa jak zwykła rura, ale przy wyznaczaniu trasy jest bardzo "droga" -
 * przedmioty wybierają ją dopiero wtedy, gdy nie ma innej drogi do celu.
 */
public class RestrictionTubeBlock extends TubeBlock {
    public static final int COST = 1000;

    public RestrictionTubeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int routeCost() {
        return COST;
    }
}
