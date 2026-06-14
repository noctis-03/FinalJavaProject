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

# 기능 구현

📎 [기능 구현 문서 바로가기](https://github.com/noctis-03/FinalJavaProject/blob/master/docs/3-feature-implementation.md)

---

## 마일스톤 구성

<style scoped>
table { font-size: 0.78rem; width: 60%; margin: 0 auto; }
table th:nth-child(1), table td:nth-child(1) { width: 25%; }
table th:nth-child(2), table td:nth-child(2) { width: 75%; }
</style>

| 버전 | 마일스톤 |
|:---:|:---|
| v0.1 | 기반 구조 및 DB |
| v0.2 | 캘린더 코어 |
| v0.3 | 과목 및 시간표 |
| v0.4 | 블록 시스템 |
| v0.5 | 미리블록 및 DnD |
| v0.6 | 무한 캔버스 |
| v0.7 | 자동화 시스템 |
| v0.8 | 외부 API |
| v1.0 | 통합 및 완성 |

---

<style scoped>
table { font-size: 0.82rem; }
table th:nth-child(1), table td:nth-child(1) { width: 12%; }
table th:nth-child(2), table td:nth-child(2) { width: 20%; }
table th:nth-child(3), table td:nth-child(3) { width: 56%; }
table th:nth-child(4), table td:nth-child(4) { width: 12%; }
</style>

### v0.2 — 캘린더 코어

| FR 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| FR-01 | 블록 기반 캘린더 페이지 (주간/월간 뷰, 일정 블록 생성·이동·리사이즈, 충돌 검사) | `CalendarPagePanel.java`, `WeekViewPanel.java`, `MonthViewPanel.java`, `CalendarController.java`, `CalendarService.java` | ☑ 완성 |

---

<style scoped>
table { font-size: 0.82rem; }
table th:nth-child(1), table td:nth-child(1) { width: 12%; }
table th:nth-child(2), table td:nth-child(2) { width: 20%; }
table th:nth-child(3), table td:nth-child(3) { width: 56%; }
table th:nth-child(4), table td:nth-child(4) { width: 12%; }
</style>

### v0.3 — 과목 및 시간표

| FR 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| FR-03 | 과목 페이지 (좌측 트리, 과목별 색상, 자동 생성) | `SubjectPagePanel.java`, `SubjectTreePanel.java`, `SubjectService.java` | ☑ 완성 |
| FR-09 | 시간표 입력 (학기/요일/시간 등록, 주간 반복 블록 자동 생성) | `TimetableInputDialog.java`, `TimetableService.java`, `TimetableDAO.java` | ☑ 완성 |
| FR-10 | 과제 입력 (마감일 캘린더 자동 연동) | `AssignmentInputDialog.java`, `AssignmentService.java`, `AssignmentDAO.java` | ☑ 완성 |

---

<style scoped>
table { font-size: 0.82rem; }
table th:nth-child(1), table td:nth-child(1) { width: 12%; }
table th:nth-child(2), table td:nth-child(2) { width: 20%; }
table th:nth-child(3), table td:nth-child(3) { width: 56%; }
table th:nth-child(4), table td:nth-child(4) { width: 12%; }
</style>

### v0.4 — 블록 시스템

| FR 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| FR-04 | 과제 블록 (상태/마감 임박/지남 표시, 캘린더 마감 연동·해제) | `AssignmentBlock.java`, `AssignmentService.java` | ☑ 완성 |
| FR-05 | 파일 블록 (로컬 파일 등록, 시스템 기본 프로그램으로 열기) | `FileBlock.java`, `FileBlockService.java`, `FileBlockComponent.java` | ☑ 완성 |
| FR-06 | 텍스트 블록 (포스트잇형 메모, 편집 다이얼로그) | `TextBlock.java`, `TextBlockComponent.java`, `TextBlockEditDialog.java` | ☑ 완성 |
| FR-07 | 링크 블록 (URL 검증, 기본 브라우저로 열기) | `LinkBlock.java`, `LinkBlockComponent.java`, `LinkBlockEditDialog.java` | ☑ 완성 |

---

<style scoped>
table { font-size: 0.82rem; }
table th:nth-child(1), table td:nth-child(1) { width: 12%; }
table th:nth-child(2), table td:nth-child(2) { width: 20%; }
table th:nth-child(3), table td:nth-child(3) { width: 56%; }
table th:nth-child(4), table td:nth-child(4) { width: 12%; }
</style>

### v0.5 — 미리블록 및 DnD

| FR 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| FR-02 | 미리블록 템플릿 (생성/삭제, 캘린더 배치, 템플릿-인스턴스 분리) | `PresetBlock.java`, `PresetBlockService.java`, `PresetBlockPanel.java`, `PresetBlockCreateDialog.java` | ☑ 완성 |
| FR-11 | 드래그 앤 드롭 조작 (미리블록→캘린더, 트리 파일→캔버스, 팔레트→캔버스) | `BlockDragSource.java`, `BlockTransferable.java`, `CalendarDropTarget.java`, `CanvasDropTarget.java` | ☑ 완성 |

---

<style scoped>
table { font-size: 0.82rem; }
table th:nth-child(1), table td:nth-child(1) { width: 12%; }
table th:nth-child(2), table td:nth-child(2) { width: 20%; }
table th:nth-child(3), table td:nth-child(3) { width: 56%; }
table th:nth-child(4), table td:nth-child(4) { width: 12%; }
</style>

### v0.6 — 무한 캔버스

| FR 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| FR-08 | 무한 캔버스 보드 (팬/줌, 격자 스냅, 자유 배치, 자동 저장) | `InfiniteCanvasPanel.java`, `CanvasViewport.java`, `GridRenderer.java`, `CanvasService.java`, `CanvasState.java` | ☑ 완성 |
| FR-16 | ~~필기기능~~ → 캔버스 블록 우선순위 (1·2·3 단계, 선택 시 메뉴, 최근 클릭 우선) | `InfiniteCanvasPanel.java`, `CanvasBlockComponent.java` | ☑ 완성 |

---

<style scoped>
table { font-size: 0.82rem; }
table th:nth-child(1), table td:nth-child(1) { width: 12%; }
table th:nth-child(2), table td:nth-child(2) { width: 20%; }
table th:nth-child(3), table td:nth-child(3) { width: 56%; }
table th:nth-child(4), table td:nth-child(4) { width: 12%; }
</style>

### v0.7 — 자동화 시스템

| FR 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| FR-12 | 자동화 시스템 (캘린더: 다음 주/요일/시각/미리블록 슬롯 / 과목: 노드 그래프 연결·실행) | `CalendarAutomationService.java`, `AutomationService.java`, `CalendarAutomationPanel.java`, `AutomationBlockComponent.java` | ☑ 완성 |
| FR-13 | 자동화 블록 (캔버스 위 실행 노드, 출력 포트 드래그로 연결) | `AutomationBlock.java`, `AutomationRule.java`, `CalendarAutomationRule.java`, `AutomationRuleDAO.java`, `CalendarAutomationDAO.java` | ☑ 완성 |

---

<style scoped>
table { font-size: 0.82rem; }
table th:nth-child(1), table td:nth-child(1) { width: 12%; }
table th:nth-child(2), table td:nth-child(2) { width: 20%; }
table th:nth-child(3), table td:nth-child(3) { width: 56%; }
table th:nth-child(4), table td:nth-child(4) { width: 12%; }
</style>

### v0.8 — 외부 API

| FR 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| FR-14 | 날씨 위젯 (Open-Meteo 사용, 대전 현재/내일 표시) | `WeatherService.java`, `NavigationPanel.java` | ☑ 완성 |
| FR-15 | ~~대학 정보~~ → 블록 외부 링크 즐겨찾기로 대체 구현 | `ExternalLinkService.java`, `ExternalLinkPanel.java`, `ExternalLinkEditDialog.java`, `ExternalLink.java` | ☑ 완성 |

---

<style scoped>
table { font-size: 0.82rem; }
table th:nth-child(1), table td:nth-child(1) { width: 12%; }
table th:nth-child(2), table td:nth-child(2) { width: 20%; }
table th:nth-child(3), table td:nth-child(3) { width: 56%; }
table th:nth-child(4), table td:nth-child(4) { width: 12%; }
</style>

### v1.0 — 통합 및 완성

| 요구사항 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| 통합-01 | 전체 프로그램 초기화 (모든 사용자 데이터 삭제·스키마 재생성) | `DatabaseManager.java`, `MainFrame.java` | ☑ 완성 |
| 통합-02 | EventBus 기반 컴포넌트 간 갱신 (일정/과제/과목 변경 시 자동 반영) | `EventBus.java` | ☑ 완성 |
| 통합-03 | 트랜잭션 처리 (블록 + 서브 테이블 원자적 저장) | `TransactionManager.java` | ☑ 완성 |
| NFR 검증 | 자동 저장, 데이터 무결성, 캔버스 상태 보존 등 | 전 영역 | ☑ 완성 |

---

<!-- _class: lead -->

# 구현 내용 설명

---

## FR-01: 블록 기반 캘린더 페이지

- **관련 소스**: `CalendarPagePanel.java`, `WeekViewPanel.java`, `CalendarService.java`
- **설명**:
  - `CalendarPagePanel`이 좌측 미리블록 / 중앙 그리드 / 우측 외부링크 레이아웃을 구성
  - 주간·월간 뷰를 `CardLayout`으로 전환
  - `CalendarService`가 충돌 검사(`ScheduleConflictException`)와 블록 CRUD를 담당

---

## FR-02: 미리블록 템플릿

- **관련 소스**: `PresetBlockService.java`, `PresetBlockPanel.java`
- **설명**:
  - 템플릿(`PresetBlock`)과 캘린더 배치 결과(`ScheduleBlock`)를 분리
  - 캘린더에 드래그하면 시작 시각 + 템플릿 소요시간으로 일정 블록을 새로 생성

---

## FR-03: 과목 페이지

- **관련 소스**: `SubjectPagePanel.java`, `SubjectTreePanel.java`
- **설명**:
  - 좌측 아코디언 트리에서 과목별 과제/파일을 펼쳐 확인
  - 우측에는 캔버스 헤더 + 무한 캔버스가 배치
  - 트리 갱신 시에도 열려있던 과목 카드를 자동 복원

---

## FR-04: 과제 블록

- **관련 소스**: `AssignmentService.java`, `AssignmentBlock.java`
- **설명**:
  - 과제 생성 시 캘린더에 마감 표시용 `ScheduleBlock`을 자동 생성
  - `memo`에 `[ASSIGNMENT_DEADLINE:id]` 태그로 연결
  - 마감 임박/지남 여부를 모델에서 판단하여 UI에 강조 표시

---

## FR-05: 파일 블록

- **관련 소스**: `FileBlockService.java`, `FileBlock.java`, `FileBlockComponent.java`
- **설명**:
  - 지원 확장자(PDF, HWP, DOCX 등)를 검증해 등록
  - `Desktop.open()`으로 시스템 기본 프로그램에서 파일 열기
  - 원본 파일 누락 시 캔버스에서 경고 표시

---

## FR-06: 텍스트 블록

- **관련 소스**: `TextBlockComponent.java`, `TextBlockEditDialog.java`
- **설명**:
  - 포스트잇 형태(기본 배경 `#FFFF99`)로 캔버스에 배치
  - 더블클릭으로 제목·본문 편집 다이얼로그 열기

---

## FR-07: 링크 블록

- **관련 소스**: `LinkBlock.java`, `LinkBlockComponent.java`
- **설명**:
  - URL 스킴이 없으면 `https://` 자동 보정
  - 표시 이름과 URL을 함께 보관
  - `Desktop.browse()`로 기본 브라우저에서 열기

---

## FR-08: 무한 캔버스 보드

- **관련 소스**: `InfiniteCanvasPanel.java`, `CanvasViewport.java`, `GridRenderer.java`
- **설명**:
  - world ↔ screen 좌표 변환(`CanvasViewport`), 격자 스냅(`GridRenderer`), 휠 줌·드래그 팬 구현
  - 위치·크기는 `canvas_state` 테이블에 자동 저장
  - "중심으로" 버튼으로 모든 블록이 보이도록 자동 줌·정렬

---

## FR-09: 시간표 입력

- **관련 소스**: `TimetableService.java`, `TimetableInputDialog.java`
- **설명**:
  - 학기·요일·시간을 입력하면 과목이 자동 생성
  - 학기 시작~종료 범위 동안 매주 반복 `ScheduleBlock`이 생성되어 캘린더에 표시
  - 시간 충돌 시 다이얼로그로 거부

---

## FR-10: 과제 입력

- **관련 소스**: `AssignmentInputDialog.java`, `SubjectController.java`
- **설명**:
  - 과목 트리의 "＋ 과제 추가"로 폼 열기
  - 마감일이 과거이면 확인 후 생성
  - 생성 시 마감 표시가 캘린더에 즉시 반영

---

## FR-11: 드래그 앤 드롭 조작

- **관련 소스**: `BlockDragSource.java`, `BlockTransferable.java`, `CalendarDropTarget.java`, `CanvasDropTarget.java`
- **설명**:
  - 자체 `DataFlavor`로 블록 id·유형을 운반
  - 캘린더 주간 뷰: 드롭 좌표를 30분 슬롯 시각으로 환산
  - 캔버스: 격자에 스냅된 world 좌표로 환산

---

## FR-12: 자동화 시스템

- **관련 소스**: `CalendarAutomationService.java`, `AutomationService.java`
- **설명**:
  - 캘린더 자동화: "다음 주/이번 주 → 요일 → 시각 → 미리블록" 슬롯을 채워 일정 등록
  - 과목 자동화: 캔버스 위 실행 블록의 출력 포트를 파일/링크에 드래그로 연결 후 ▶로 한 번에 실행

---

## FR-13: 자동화 블록

- **관련 소스**: `AutomationBlockComponent.java`, `AutomationRuleDAO.java`
- **설명**:
  - `automation_rule + command + slot + slot_open_blocks` 4단 스키마를 DAO가 자동으로 처리
  - 캔버스에서는 연결된 블록 수가 헤더에 표시

---

## FR-14: 날씨

- **관련 소스**: `WeatherService.java`, `NavigationPanel.java`
- **설명**:
  - Open-Meteo API(키 불필요)를 비동기 호출
  - 대전의 현재 기온·최고/최저, 내일 예보를 상단 네비에 카드로 표시
  - 10분 캐시 + 수동 새로고침 버튼

---

## FR-15: ~~대학 정보 블록~~ → 즐겨찾기

- **관련 소스**: `ExternalLinkService.java`, `ExternalLinkPanel.java`
- **설명**:
  - 장학금/공지 크롤링 대신, 사용자가 직접 등록·관리하는 외부 링크 즐겨찾기로 대체 구현
  - 기본값으로 한남대학교 홈페이지가 시드됨

---

## FR-16: ~~필기 기능~~ → 블록 우선순위 시스템

- **관련 소스**: `InfiniteCanvasPanel.java`, `CanvasBlockComponent.java`
- **설명**:
  - 필기는 이 시스템에 어울리지 않다고 판단하여 대체 시스템으로 추가
  - 블록 선택 시 상단 우측에 1/2/3 버튼 표시 (1이 가장 위에 그려짐)
  - 같은 우선순위에서는 최근 클릭한 쪽이 위로
  - 값은 `canvas_state.z_order`에 영속화

---

## 통합 구현

| 항목 | 내용 |
|:---:|:---|
| **통합-01** | 상단 "⟲ 초기화" 버튼 → 2단계 확인 후 모든 사용자 테이블 DROP + 스키마 재생성 + 메인 프레임 재시작 |
| **통합-02** | `EventBus`를 통해 일정/과제/과목 변경 시 관련 뷰가 자동으로 갱신됨 |
| **통합-03** | `TransactionManager`로 블록 + 서브 테이블을 원자적으로 저장하여 데이터 무결성 보장 |
| **NFR 검증** | 자동 저장, 데이터 무결성, 캔버스 상태 보존 등 전 영역 검증 완료 |