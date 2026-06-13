---
marp: true
theme: default
paginate: true
size: 16:9
header: '대학생을 위한 블록 기반 학업 통합 관리 프로그램'
footer: '자바프로그래밍 | 20220540 김가온'
---

<!-- _class: lead -->
<!-- _paginate: false -->

# 4. 요구사항

📎 [요구사항 분석 문서 바로가기](https://github.com/noctis-03/FinalJavaProject/blob/master/docs/1-requirement-analysis.md)

---

## v0.1 — 기반 구조 및 DB

프로젝트 전체 골격과 데이터베이스 스키마를 설계하고 구축하는 단계입니다.

---

<style scoped>
table {
  font-size: 0.85rem;
}
table th:nth-child(1), table td:nth-child(1) {
  width: 12%;
}
table th:nth-child(2), table td:nth-child(2) {
  width: 23%;
}
table th:nth-child(3), table td:nth-child(3) {
  width: 65%;
}
</style>

### v0.2 — 캘린더 코어

| FR 번호 | 이름 | 설명 |
|---|---|---|
| FR-01 | 블록 기반 캘린더 페이지 | 블록 단위로 일정을 생성, 배치, 이동할 수 있는 기본적인 캘린더 시스템 페이지를 구현합니다. |

---

<style scoped>
table {
  font-size: 0.85rem;
}
table th:nth-child(1), table td:nth-child(1) {
  width: 12%;
}
table th:nth-child(2), table td:nth-child(2) {
  width: 23%;
}
table th:nth-child(3), table td:nth-child(3) {
  width: 65%;
}
</style>

### v0.3 — 과목 및 시간표

| FR 번호 | 이름 | 설명                                   |
|---|---|--------------------------------------|
| FR-03 | 과목 페이지 | 과목별로 과제, 자료를 구조화하여 관리하는 전용 페이지를 제공합니다. |
| FR-09 | 시간표 입력 | 학기별 수강 과목과 시간을 입력하고 관리할 수 있습니다.      |
| FR-10 | 과제 입력 | 캘린더 페이지의 과제 블록으로 생성됩니다.              |

---

<style scoped>
table {
  font-size: 0.85rem;
}
table th:nth-child(1), table td:nth-child(1) {
  width: 12%;
}
table th:nth-child(2), table td:nth-child(2) {
  width: 23%;
}
table th:nth-child(3), table td:nth-child(3) {
  width: 65%;
}
</style>

## v0.4 — 블록 시스템

| FR 번호 | 이름 | 설명 |
|---|---|---|
| FR-04 | 과제 블록 | 과제 생성 시 마감일이 캘린더에 자동 연동되고, 작업 일정을 배치할 수 있습니다. |
| FR-05 | 파일 블록 | 강의 자료, 제출 파일을 블록 단위로 정리하고 관리합니다. |
| FR-06 | 텍스트 블록 | 텍스트를 입력할 수 있는 블록입니다. |
| FR-07 | 링크 블록 | 입력한 인터넷 주소로 이동할 수 있는 블록입니다. |

---

<style scoped>
table {
  font-size: 0.85rem;
}
table th:nth-child(1), table td:nth-child(1) {
  width: 12%;
}
table th:nth-child(2), table td:nth-child(2) {
  width: 23%;
}
table th:nth-child(3), table td:nth-child(3) {
  width: 65%;
}
</style>

## v0.5 — 미리블록 및 DnD

| FR 번호 | 이름 | 설명 |
|---|---|---|
| FR-02 | 미리블록 템플릿 | 자주 사용하는 일정을 재사용 가능한 블록 템플릿으로 저장하고 배치합니다. |
| FR-11 | 드래그 앤 드롭 조작 | 블록의 생성, 이동, 삭제를 드래그 앤 드롭으로 수행할 수 있습니다. |

---

<style scoped>
table {
  font-size: 0.85rem;
}
table th:nth-child(1), table td:nth-child(1) {
  width: 12%;
}
table th:nth-child(2), table td:nth-child(2) {
  width: 23%;
}
table th:nth-child(3), table td:nth-child(3) {
  width: 65%;
}
</style>

## v0.6 — 무한 캔버스

| FR 번호 | 이름 | 설명 |
|---|---|---|
| FR-08 | 무한 캔버스 보드 | 파일 블록을 보드 위에 자유롭게 배치하여 멀티태스킹을 지원합니다. |

---

<style scoped>
table {
  font-size: 0.85rem;
}
table th:nth-child(1), table td:nth-child(1) {
  width: 12%;
}
table th:nth-child(2), table td:nth-child(2) {
  width: 23%;
}
table th:nth-child(3), table td:nth-child(3) {
  width: 65%;
}
</style>

## v0.7 — 자동화 시스템

| FR 번호 | 이름 | 설명 |
|---|---|---|
| FR-12 | 자동화 시스템 | 자동화 블록을 통해 작업을 실행하는 시스템입니다. |
| FR-13 | 자동화 블록 | 자동화 시스템에서 사용할 프롬프트형 블록입니다. |

---

<style scoped>
table {
  font-size: 0.85rem;
}
table th:nth-child(1), table td:nth-child(1) {
  width: 12%;
}
table th:nth-child(2), table td:nth-child(2) {
  width: 23%;
}
table th:nth-child(3), table td:nth-child(3) {
  width: 65%;
}
</style>

## v0.8 — 외부 API 및 사이트 활용

| FR 번호 | 이름          | 설명              |
|---|-------------|-----------------|
| FR-14 | 날씨          | 날씨를 표시합니다.      |
| FR-15 | 외부 사이트 바로가기 | 외부 사이트로 링크합니다.  |

---

## v1.0 — 통합 및 완성

통합 테스트, 비기능 요구사항(NFR) 검증을 거쳐 최종 제출을 완료합니다.
