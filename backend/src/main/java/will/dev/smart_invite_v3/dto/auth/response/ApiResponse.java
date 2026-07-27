package will.dev.smart_invite_v3.dto.auth.response;


import lombok.Builder;


@Builder
public record ApiResponse<T>(boolean success, T data, String message) {

    /**
     * Réponse succès avec données
     */
    public static <T> ApiResponse<T> success(
            T data,
            String message
    ) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    /**
     * Réponse succès sans données
     */
    public static <T> ApiResponse<T> success(
            String message
    ) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Réponse erreur simple
     */
    public static <T> ApiResponse<T> error(
            String message
    ) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    /**
     * Réponse erreur avec détails
     */
    public static <T> ApiResponse<T> error(
            T data
    ) {
        return ApiResponse.<T>builder()
                .success(false)
                .data(data)
                .build();
    }

}