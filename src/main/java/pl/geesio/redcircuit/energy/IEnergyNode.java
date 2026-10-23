package pl.geesio.redcircuit.energy;

/** Węzeł sieci energii (przewód lub akumulator): sąsiednie węzły wyrównują poziom naładowania. */
public interface IEnergyNode {
    EnergyBuffer buffer();

    /** Maksymalny przepływ na stronę w jednym ticku. */
    int rate();
}
