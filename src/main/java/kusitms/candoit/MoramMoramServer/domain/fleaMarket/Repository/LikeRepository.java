package kusitms.candoit.MoramMoramServer.domain.fleaMarket.Repository;

import kusitms.candoit.MoramMoramServer.domain.fleaMarket.Entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Long countByMarketId(Long market_id);
    Optional<Like> findByMarketIdAndUserId(Long market_id,Long user_id);
    List<Like> findByUserId(Long user_id);
}
