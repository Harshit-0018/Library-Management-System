package com.library.lms.service;

import com.library.lms.dto.MemberRequest;
import com.library.lms.entity.Member;
import com.library.lms.exception.MemberNotFoundException;
import com.library.lms.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Member createMember(MemberRequest request) {
        Member member = Member.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
        Member saved = memberRepository.save(member);
        log.info("Created member id={} name='{}'", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> MemberNotFoundException.forId(id));
    }
}
