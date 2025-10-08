package guru.nicks.designpattern;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Builder nested in another one, with {@link #and()} bubbling up to the parent. For Lombok-managed builders,
 * {@link #build()} is auto-generated.
 *
 * @param <T> type produced by this builder
 * @param <P> parent builder type
 */
@RequiredArgsConstructor
public abstract class NestedBuilder<T, P> {

    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private final P parentBuilder;

    /**
     * Builds and returns the configured object.
     *
     * @return object just built
     */
    public abstract T build();

    /**
     * Returns to the parent builder to continue building the parent object.
     *
     * @return parent builder instance
     */
    public P and() {
        return parentBuilder;
    }

}
