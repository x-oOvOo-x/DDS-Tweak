package ddstweak.mixin.integration.ommc;

/**
 * Explicit DT development-version policy cross-checked against the AyakaCraft
 * OMMC source tree (calboot/dev) and its per-version Gradle projects.
 * Upstream availability is necessary, never sufficient: bytecode probing
 * must independently verify that the installed sorter is safe to intercept.
 */
public final class OmmcSortVersionMatrix {
    private OmmcSortVersionMatrix(){}

    public enum Status {
        SOURCE_BACKED,
        NO_MATCHING_UPSTREAM_BUILD,
        ADAPTER_REQUIRED,
        UNKNOWN
    }

    public static Status status(String minecraftVersion){
        if(minecraftVersion==null)return Status.UNKNOWN;
        switch(minecraftVersion){
            case "1.14.4":
            case "1.15.2":
            case "1.16.5":
            case "1.17.1":
            case "1.18.2":
            case "1.19.4":
            case "1.20.1":
            case "1.20.6":
            case "1.21.1":
            case "1.21.3":
            case "1.21.4":
            case "1.21.5":
            case "1.21.8":
            case "1.21.10":
            case "1.21.11":
                return Status.SOURCE_BACKED;
            case "1.20.2":
            case "1.20.4":
            case "1.21.9":
                return Status.NO_MATCHING_UPSTREAM_BUILD;
            case "26.1.2":
            case "26.2":
            case "26.3":
                return Status.ADAPTER_REQUIRED;
            default:
                return Status.UNKNOWN;
        }
    }

    public static boolean canSort(String minecraftVersion){
        Status status=status(minecraftVersion);
        return status==Status.SOURCE_BACKED||status==Status.ADAPTER_REQUIRED;
    }
}
