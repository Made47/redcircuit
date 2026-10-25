package pl.geesio.redcircuit.craft;


/**
 * Nożyce do drutu (Wire Cutters): narzędzie do wytwarzania cienkich drutów ze sztabek (zostaje w siatce, traci wytrzymałość).
 * Tak jak piła - zostaje w siatce i traci 1 punkt wytrzymałości za każde użycie.
 */
public class WireCuttersItem extends HandsawItem {
    public WireCuttersItem(Properties properties) {
        super(properties);
    }
}
