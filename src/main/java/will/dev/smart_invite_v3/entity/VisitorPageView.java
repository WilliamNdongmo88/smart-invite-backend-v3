package will.dev.smart_invite_v3.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "visitor_page_views", indexes = {
        @Index(name = "idx_pageviews_session",    columnList = "session_id"),
        @Index(name = "idx_pageviews_viewed_at",  columnList = "viewed_at"),
        @Index(name = "idx_pageviews_page_url",   columnList = "page_url")
})
public class VisitorPageView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private VisitorSession session;

    @Column(name = "page_url", columnDefinition = "TEXT")
    private String pageUrl;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;
}
