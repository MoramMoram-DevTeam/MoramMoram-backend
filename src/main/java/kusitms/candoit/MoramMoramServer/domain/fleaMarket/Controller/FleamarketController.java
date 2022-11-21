package kusitms.candoit.MoramMoramServer.domain.fleaMarket.Controller;

import kusitms.candoit.MoramMoramServer.domain.fleaMarket.Dto.FleamarketDto;
import kusitms.candoit.MoramMoramServer.domain.fleaMarket.Entity.Fleamarket;
import kusitms.candoit.MoramMoramServer.domain.fleaMarket.Entity.HostPost;
import kusitms.candoit.MoramMoramServer.domain.fleaMarket.Entity.Like;
import kusitms.candoit.MoramMoramServer.domain.fleaMarket.Service.FleamarketService;
import kusitms.candoit.MoramMoramServer.global.Model.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FleamarketController {

    private final FleamarketService fleamarketService;

    @GetMapping("markets")
    public ResponseEntity<List<Object>> mainpage() {
        return fleamarketService.mainpage();
    }

/*    @GetMapping("markets/{marketId}")
    public ResponseEntity<FleamarketDto.detail> detailpage(
            @PathVariable Long marketId
    ){
        return fleamarketService.detailpage(marketId);
    }*/

    // 상세조회
    @GetMapping("markets/")
    public ResponseEntity<FleamarketDto.detail> detailpage(
            @RequestParam Long m_id
    ) {
        return fleamarketService.detailpage(m_id);
    }

    @GetMapping("markets/search")
    public ResponseEntity<List<Fleamarket>> searchpage(
            @RequestParam String m_name
    ) {
        return fleamarketService.searchpage(m_name);
    }


    // 찜한 플리마켓 보기
    @GetMapping("/wish-markets")
    @PreAuthorize("hasAnyRole('ADMIN','USER','OFFICE')")
    public ResponseEntity<List<Like>> like_list() {
        return fleamarketService.like_list();
    }

    // 찜기능
    @PostMapping("wish")
    @PreAuthorize("hasAnyRole('ADMIN','USER','OFFICE')")
    public ResponseEntity<Status> itemLike(
            @RequestBody FleamarketDto.like request
    ) {
        return fleamarketService.itemLike(request);
    }

    // 주최 글 작성
    @PostMapping("markets/register")
    @PreAuthorize("hasAnyRole('OFFICE')")
    public ResponseEntity<Status> hostpost_add(
            @RequestBody FleamarketDto.hostpost_add request
    ) {
        return fleamarketService.hostpost_add(request);
    }

    // 주최 글 조회
    @GetMapping("my-registrations")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICE')")
    public ResponseEntity<List<HostPost>> hostpost_read() {
        return fleamarketService.hostpost_read();
    }

    // 주최 글 수정
    @PatchMapping("markets/edit")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICE')")
    public ResponseEntity<Status> hostpost_edit(
            @RequestParam Long m_id,
            @RequestBody FleamarketDto.hostpost_edit request
    ){
        return fleamarketService.hostpost_edit(m_id,request);
    }

    @DeleteMapping("markets/delete")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICE')")
    public ResponseEntity<Status> hostpost_delete(
            @RequestParam Long m_id
    ){
        return fleamarketService.hostpost_delete(m_id);
    }
}
