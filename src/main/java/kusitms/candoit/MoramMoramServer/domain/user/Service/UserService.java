package kusitms.candoit.MoramMoramServer.domain.user.Service;

import kusitms.candoit.MoramMoramServer.domain.user.Dto.TokenInfoResponseDto;
import kusitms.candoit.MoramMoramServer.domain.user.Dto.UserDto;
import kusitms.candoit.MoramMoramServer.domain.user.Entity.Authority;
import kusitms.candoit.MoramMoramServer.domain.user.Entity.User;
import kusitms.candoit.MoramMoramServer.domain.user.Repository.UserRepository;
import kusitms.candoit.MoramMoramServer.global.Exception.CustomException;
import kusitms.candoit.MoramMoramServer.global.Exception.ServerException;
import kusitms.candoit.MoramMoramServer.global.Model.Status;
import kusitms.candoit.MoramMoramServer.global.config.Jwt.SecurityUtil;
import kusitms.candoit.MoramMoramServer.global.config.Jwt.TokenProvider;
import kusitms.candoit.MoramMoramServer.global.config.RedisDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;

import static kusitms.candoit.MoramMoramServer.global.Exception.CustomErrorCode.*;
import static kusitms.candoit.MoramMoramServer.global.Model.Status.LOGOUT_TRUE;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final RedisDao redisDao;

    // Validate 및 단순화 메소드

    private void LOGIN_VALIDATE(UserDto.login request) {
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new CustomException(LOGIN_FALSE)
                );

        if (request.getPw().equals("google"))
            throw new CustomException(NOT_SOCIAL_LOGIN);

        if (request.getPw().equals("kakao"))
            throw new CustomException(NOT_SOCIAL_LOGIN);

        if (!passwordEncoder.matches(
                request.getPw(),
                userRepository.findByEmail(request.getEmail())
                        .orElseThrow(
                                () -> new CustomException(LOGIN_FALSE)
                        ).getPw())
        ) {
            throw new CustomException(LOGIN_FALSE);
        }
    }

    private void REGISTER_VALIDATION(UserDto.register request) {
/*        if (request.getEmail() == null || request.getPw() == null || request.getName() == null
                || request.getWeight() == null || request.getHeight() == null)
            throw new CustomException(REGISTER_INFO_NULL);*/
        if(request.getEmail().contains("gmail") || request.getEmail().contains("daum")){
            throw new CustomException(WANT_SOCIAL_REGISTER);
        }

        if (userRepository.existsByEmail(request.getEmail()))
            throw new CustomException(DUPLICATE_USER);

        if (!request.getEmail().contains("@"))
            throw new CustomException(NOT_EMAIL_FORM);

        if (!(request.getPw().length() > 5))
            throw new CustomException(PASSWORD_SIZE_ERROR);

        if (!(request.getPw().contains("!") || request.getPw().contains("@") || request.getPw().contains("#")
                || request.getPw().contains("$") || request.getPw().contains("%") || request.getPw().contains("^")
                || request.getPw().contains("&") || request.getPw().contains("*") || request.getPw().contains("(")
                || request.getPw().contains(")"))
        ) {
            throw new CustomException(NOT_CONTAINS_EXCLAMATIONMARK);
        }
    }

    // Service
    // 회원가입
    @Transactional
    public ResponseEntity<UserDto.registerResponse> register(UserDto.register request) {
        Authority authority = Authority.builder()
                .authorityName("ROLE_USER")
                .build();

        REGISTER_VALIDATION(request);
        userRepository.save(
                User.builder()
                        .name(request.getName())
                        .email(request.getEmail())
                        .pw(passwordEncoder.encode(request.getPw()))
                        .pnum(request.getPnum())
                        .uimg(request.getUimg())
                        .seller(false)
                        .report(0)
                        .marketing(request.getMarketing())
                        .authorities(Collections.singleton(authority))
                        .build()
        );

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPw());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String atk = tokenProvider.createToken(authentication);
        String rtk = tokenProvider.createRefreshToken(request.getEmail());

        redisDao.setValues(request.getEmail(), rtk, Duration.ofDays(14));

        return new ResponseEntity<>(UserDto.registerResponse.response(
                request.getName(),
                request.getEmail(),
                atk,
                rtk
        ), HttpStatus.OK);
    }

    //로그인
    @Transactional
    public ResponseEntity<UserDto.loginResponse> login(UserDto.login request) {
        LOGIN_VALIDATE(request);
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPw());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String atk = tokenProvider.createToken(authentication);
        String rtk = tokenProvider.createRefreshToken(request.getEmail());

        redisDao.setValues(request.getEmail(), rtk, Duration.ofDays(14));

        return new ResponseEntity<>(UserDto.loginResponse.response(
                atk,
                rtk
        ), HttpStatus.OK);
    }

    // accessToken 재발급
    @Transactional
    public ResponseEntity<UserDto.loginResponse> reissue(String rtk) {
        String username = tokenProvider.getRefreshTokenInfo(rtk);
        String rtkInRedis = redisDao.getValues(username);

        if (Objects.isNull(rtkInRedis) || !rtkInRedis.equals(rtk))
            throw new ServerException(REFRESH_TOKEN_IS_BAD_REQUEST); // 410

        return new ResponseEntity<>(UserDto.loginResponse.response(
                tokenProvider.reCreateToken(username),
                null
        ), HttpStatus.OK);
    }

    // 로그아웃
    public ResponseEntity<Status> logout(String auth) {
        String atk = auth.substring(7);
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        if (redisDao.getValues(email) != null) {
            redisDao.deleteValues(email);
        }

        redisDao.setValues(atk, "logout", Duration.ofMillis(
                tokenProvider.getExpiration(atk)
        ));
        return new ResponseEntity<>(LOGOUT_TRUE, HttpStatus.OK);
    }
}
