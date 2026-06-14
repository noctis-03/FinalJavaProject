# 자바 프로그래밍 기말 프로젝트

## #3 기능 구현


| 항목 |     내용     |
|:----:|:----------:|
| **과목명** |   자바프로그래밍  |
| **학과** |   정보통신공학과  |
| **학번** |  20220540  |
| **이름** |     김가온    |
| **생성일** | 2026.05.13 |



<br>

---



##  <0. 목차>

- 1. 기능 구현
  - [1.1 마일스톤 v0.1 - 기반 구조 및 DB](#11-마일스톤-v01---기반-구조-및-db)
  - [1.2 마일스톤 v0.2 - 캘린더 코어](#12-마일스톤-v02---캘린더-코어)
  - [1.3 마일스톤 v0.3 - 과목 및 시간표](#13-마일스톤-v03---과목-및-시간표)
  - [1.4 마일스톤 v0.4 - 블록 시스템](#14-마일스톤-v04---블록-시스템)
  - [1.5 마일스톤 v0.5 - 미리블록](#15-마일스톤-v05---미리블록)
  - [1.6 마일스톤 v0.6 - 무한 캔버스](#16-마일스톤-v06---무한-캔버스)
  - [1.7 마일스톤 v0.7 - 자동화 시스템](#17-마일스톤-v07---자동화-시스템)
  - [1.8 마일스톤 v0.8 - 외부 API](#18-마일스톤-v08---외부-api)
  - [1.9 마일스톤 v0.9 - 블럭의 우선순위](#19-마일스톤-v09---블럭의-우선순위)
  - [2.0 마일스톤 v1.0 - 통합 및 완성](#110-마일스톤-v10---통합-및-완성)
- [2. 구현 내용 설명](#2-구현-내용-설명)

<br>

---



##  <1. 기능 구현>

### 1.1 마일스톤 v0.1 - 기반 구조 및 DB
- 프로젝트 골격, DB 스키마 구현
- 해당되는 요구사항 없음

<br>

### 1.2 마일스톤 v0.2 - 캘린더 코어

| 요구사항 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| FR-01 | 블록 기반 캘린더 페이지 (주간/월간 뷰, 일정 블록 생성·이동·리사이즈, 충돌 검사) | `view/calendar/CalendarPagePanel.java`, `view/calendar/WeekViewPanel.java`, `view/calendar/MonthViewPanel.java`, `controller/CalendarController.java`, `service/CalendarService.java` | ☑ 완성 |

<br>

### 1.3 마일스톤 v0.3 - 과목 및 시간표

| 요구사항 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| FR-03 | 과목 페이지 (좌측 트리, 과목별 색상, 자동 생성) | `view/subject/SubjectPagePanel.java`, `view/subject/SubjectTreePanel.java`, `service/SubjectService.java` | ☑ 완성 |
| FR-09 | 시간표 입력 (학기/요일/시간 등록, 주간 반복 블록 자동 생성) | `view/subject/TimetableInputDialog.java`, `service/TimetableService.java`, `database/dao/TimetableDAO.java` | ☑ 완성 |
| FR-10 | 과제 입력 (마감일 캘린더 자동 연동) | `view/subject/AssignmentInputDialog.java`, `service/AssignmentService.java`, `database/dao/AssignmentDAO.java` | ☑ 완성 |

<br>

### 1.4 마일스톤 v0.4 - 블록 시스템

| 요구사항 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| FR-04 | 과제 블록 (상태/마감 임박/지남 표시, 캘린더 마감 연동·해제) | `model/block/AssignmentBlock.java`, `service/AssignmentService.java` | ☑ 완성 |
| FR-05 | 파일 블록 (로컬 파일 등록, 시스템 기본 프로그램으로 열기) | `model/block/FileBlock.java`, `service/FileBlockService.java`, `view/canvas/FileBlockComponent.java` | ☑ 완성 |
| FR-06 | 텍스트 블록 (포스트잇형 메모, 편집 다이얼로그) | `model/block/TextBlock.java`, `view/canvas/TextBlockComponent.java`, `view/canvas/TextBlockEditDialog.java` | ☑ 완성 |
| FR-07 | 링크 블록 (URL 검증, 기본 브라우저로 열기) | `model/block/LinkBlock.java`, `view/canvas/LinkBlockComponent.java`, `view/canvas/LinkBlockEditDialog.java` | ☑ 완성 |

<br>

### 1.5 마일스톤 v0.5 - 미리블록

| 요구사항 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| FR-02 | 미리블록 템플릿 (생성/삭제, 캘린더 배치, 템플릿-인스턴스 분리) | `model/block/PresetBlock.java`, `service/PresetBlockService.java`, `view/calendar/PresetBlockPanel.java`, `view/calendar/PresetBlockCreateDialog.java` | ☑ 완성 |
| FR-11 | 드래그 앤 드롭 조작 (미리블록→캘린더, 트리 파일→캔버스, 팔레트→캔버스) | `dnd/BlockDragSource.java`, `dnd/BlockTransferable.java`, `dnd/CalendarDropTarget.java`, `dnd/CanvasDropTarget.java` | ☑ 완성 |

<br>

### 1.6 마일스톤 v0.6 - 무한 캔버스

| 요구사항 번호 | 간단 설명                                | 관련 소스                                                                                                                                                                     | 구현 여부 |
|:---:|:-------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:---:|
| FR-08 | 무한 캔버스 보드 (팬/줌, 격자 스냅, 자유 배치, 자동 저장) | `view/canvas/InfiniteCanvasPanel.java`, `view/canvas/CanvasViewport.java`, `view/canvas/GridRenderer.java`, `service/CanvasService.java`, `model/canvas/CanvasState.java` | ☑ 완성 |
| FR-16 | ~~필기 기능~~ -> 블록의 우선순위 설정 시스템         | 구현                                                                                                                                                                        | ✕ 미완성 |

<br>

### 1.7 마일스톤 v0.7 - 자동화 시스템

| 요구사항 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| FR-12 | 자동화 시스템 (캘린더: 다음 주/요일/시각/미리블록 슬롯 / 과목: 노드 그래프 연결·실행) | `service/CalendarAutomationService.java`, `service/AutomationService.java`, `view/calendar/CalendarAutomationPanel.java`, `view/canvas/AutomationBlockComponent.java` | ☑ 완성 |
| FR-13 | 자동화 블록 (캔버스 위 실행 노드, 출력 포트 드래그로 연결) | `model/block/AutomationBlock.java`, `model/automation/AutomationRule.java`, `model/automation/CalendarAutomationRule.java`, `database/dao/AutomationRuleDAO.java`, `database/dao/CalendarAutomationDAO.java` | ☑ 완성 |

<br>

### 1.8 마일스톤 v0.8 - 외부 API

| 요구사항 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| FR-14 | 날씨 위젯 (Open-Meteo 사용, 대전 현재/내일 표시) | `service/WeatherService.java`, `view/NavigationPanel.java` | ☑ 완성 |
| FR-15 | 대학 정보 블록 (외부 링크 즐겨찾기로 대체 구현 — 홈페이지/공지 등 빠른 접근) | `service/ExternalLinkService.java`, `view/calendar/ExternalLinkPanel.java`, `view/calendar/ExternalLinkEditDialog.java`, `model/ExternalLink.java` | ◇ 부분완성 |

<br>

### 1.9 마일스톤 v0.9 - 블럭의 우선순위

| 요구사항 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| 추가-01 | 캔버스 블록 우선순위 (1·2·3 단계, 선택 시 떠 있는 1/2/3 메뉴, 같은 우선순위 내 최근 클릭 우선) | `view/canvas/InfiniteCanvasPanel.java`, `view/canvas/CanvasBlockComponent.java` | ☑ 완성 |
| 추가-02 | 자동화 연결선 최상위 렌더링 (블록 위로 항상 보이도록) | `view/canvas/InfiniteCanvasPanel.java` | ☑ 완성 |

<br>

### 1.10 마일스톤 v1.0 - 통합 및 완성

| 요구사항 번호 | 간단 설명 | 관련 소스 | 구현 여부 |
|:---:|:---|:---|:---:|
| 통합-01 | 전체 프로그램 초기화 기능 (모든 사용자 데이터 삭제·스키마 재생성) | `database/DatabaseManager.java` (`wipeAllData`), `view/MainFrame.java` (`onResetApp`) | ☑ 완성 |
| 통합-02 | EventBus 기반 컴포넌트 간 갱신 (일정/과제/과목 변경 시 자동 반영) | `common/EventBus.java` | ☑ 완성 |
| 통합-03 | 트랜잭션 처리 (블록 + 서브 테이블 원자적 저장) | `database/TransactionManager.java` | ☑ 완성 |
| NFR 검증 | 자동 저장, 데이터 무결성, 캔버스 상태 보존 등 | 전 영역 | ☑ 완성 |

<br>

---

## <2. 구현 내용 설명>

각 요구사항에 대해 구현 내용을 간략히 서술한다.

<br>

**FR-01: 블록 기반 캘린더 페이지**
- **구현 여부**: ☑ 완성
- **관련 소스**: `view/calendar/CalendarPagePanel.java`, `view/calendar/WeekViewPanel.java`, `service/CalendarService.java`
- **설명**: `CalendarPagePanel`이 좌측 미리블록 / 중앙 그리드 / 우측 외부링크 레이아웃을 구성하고, 주간·월간 뷰를 `CardLayout`으로 전환. `CalendarService`가 충돌 검사(`ScheduleConflictException`)와 블록 CRUD를 담당.

<br>

**FR-02: 미리블록 템플릿**
- **구현 여부**: ☑ 완성
- **관련 소스**: `service/PresetBlockService.java`, `view/calendar/PresetBlockPanel.java`
- **설명**: 템플릿(`PresetBlock`)과 캘린더 배치 결과(`ScheduleBlock`)를 분리. 캘린더에 드래그하면 시작 시각 + 템플릿 소요시간으로 일정 블록을 새로 생성한다.

<br>

**FR-03: 과목 페이지**
- **구현 여부**: ☑ 완성
- **관련 소스**: `view/subject/SubjectPagePanel.java`, `view/subject/SubjectTreePanel.java`
- **설명**: 좌측 아코디언 트리에서 과목별 과제/파일을 펼쳐 보고, 우측에는 캔버스 헤더 + 무한 캔버스가 배치됨. 트리 갱신 시에도 열려있던 과목 카드를 자동 복원.

<br>

**FR-04: 과제 블록**
- **구현 여부**: ☑ 완성
- **관련 소스**: `service/AssignmentService.java`, `model/block/AssignmentBlock.java`
- **설명**: 과제 생성 시 캘린더에 마감 표시용 `ScheduleBlock`을 자동 생성(memo에 `[ASSIGNMENT_DEADLINE:id]` 태그). 마감 임박/지남 여부를 모델에서 판단하여 UI에 강조 표시.

<br>

**FR-05: 파일 블록**
- **구현 여부**: ☑ 완성
- **관련 소스**: `service/FileBlockService.java`, `model/block/FileBlock.java`, `view/canvas/FileBlockComponent.java`
- **설명**: 지원 확장자(PDF, HWP, DOCX 등)를 검증해 등록하고 `Desktop.open()`으로 시스템 기본 프로그램에서 연다. 원본 누락 시 캔버스에서 경고 표시.

<br>

**FR-06: 텍스트 블록**
- **구현 여부**: ☑ 완성
- **관련 소스**: `view/canvas/TextBlockComponent.java`, `view/canvas/TextBlockEditDialog.java`
- **설명**: 포스트잇 형태(기본 배경 `#FFFF99`)로 캔버스에 배치. 더블클릭으로 제목·본문 편집 다이얼로그가 열린다.

<br>

**FR-07: 링크 블록**
- **구현 여부**: ☑ 완성
- **관련 소스**: `model/block/LinkBlock.java`, `view/canvas/LinkBlockComponent.java`
- **설명**: URL 스킴이 없으면 `https://` 자동 보정. 표시 이름과 URL을 함께 보관하며 `Desktop.browse()`로 기본 브라우저에서 연다.

<br>

**FR-08: 무한 캔버스 보드**
- **구현 여부**: ☑ 완성
- **관련 소스**: `view/canvas/InfiniteCanvasPanel.java`, `view/canvas/CanvasViewport.java`, `view/canvas/GridRenderer.java`
- **설명**: world ↔ screen 좌표 변환(`CanvasViewport`), 격자 스냅(`GridRenderer`), 휠 줌·드래그 팬을 구현. 위치·크기는 `canvas_state` 테이블에 자동 저장. "중심으로" 버튼으로 모든 블록이 보이도록 자동 줌·정렬.

<br>

**FR-09: 시간표 입력**
- **구현 여부**: ☑ 완성
- **관련 소스**: `service/TimetableService.java`, `view/subject/TimetableInputDialog.java`
- **설명**: 학기·요일·시간을 입력하면 과목이 자동 생성되고, 학기 시작~종료 범위 동안 매주 반복 `ScheduleBlock`이 생성되어 캘린더에 표시됨. 시간 충돌 시 다이얼로그로 거부.

<br>

**FR-10: 과제 입력**
- **구현 여부**: ☑ 완성
- **관련 소스**: `view/subject/AssignmentInputDialog.java`, `controller/SubjectController.java`
- **설명**: 과목 트리의 "＋ 과제 추가"로 폼을 열고, 마감일이 과거이면 확인 후 생성. 생성 시 마감 표시가 캘린더에 즉시 반영.

<br>

**FR-11: 드래그 앤 드롭 조작**
- **구현 여부**: ☑ 완성
- **관련 소스**: `dnd/BlockDragSource.java`, `dnd/BlockTransferable.java`, `dnd/CalendarDropTarget.java`, `dnd/CanvasDropTarget.java`
- **설명**: 자체 `DataFlavor`로 블록 id·유형을 운반. 캘린더 주간 뷰는 드롭 좌표를 30분 슬롯 시각으로 환산하고, 캔버스는 격자에 스냅된 world 좌표로 환산.

<br>

**FR-12: 자동화 시스템**
- **구현 여부**: ☑ 완성
- **관련 소스**: `service/CalendarAutomationService.java`, `service/AutomationService.java`
- **설명**: 캘린더 자동화는 "다음 주/이번 주 → 요일 → 시각 → 미리블록" 슬롯을 채워 일정을 등록. 과목 자동화는 캔버스 위 실행 블록의 출력 포트를 파일/링크에 드래그로 연결하고 ▶로 한 번에 열기.

<br>

**FR-13: 자동화 블록**
- **구현 여부**: ☑ 완성
- **관련 소스**: `view/canvas/AutomationBlockComponent.java`, `database/dao/AutomationRuleDAO.java`
- **설명**: `automation_rule + command + slot + slot_open_blocks` 4단 스키마를 DAO가 자동으로 채워 단순화. 캔버스에서는 연결된 블록 수가 헤더에 표시됨.

<br>

**FR-14: 날씨**
- **구현 여부**: ☑ 완성
- **관련 소스**: `service/WeatherService.java`, `view/NavigationPanel.java`
- **설명**: Open-Meteo API(키 불필요)를 비동기 호출해 대전의 현재 기온·최고/최저, 내일 예보를 상단 네비에 카드로 표시. 10분 캐시 + 수동 새로고침 버튼.

<br>

**FR-15: 대학 정보 블록**
- **구현 여부**: ◇ 부분완성
- **관련 소스**: `service/ExternalLinkService.java`, `view/calendar/ExternalLinkPanel.java`
- **설명**: 장학금/공지 등을 일일이 크롤링하는 대신, 사용자가 직접 등록·관리할 수 있는 외부 링크 즐겨찾기로 대체 구현. 기본값으로 한남대학교 홈페이지가 시드됨.

<br>

**FR-16: 필기 기능**
- **구현 여부**: ✕ 미완성
- **관련 소스**: 미구현
- **설명**: 요구사항 단계에서 보류로 분류한 항목으로, 펜/지우개 도구는 구현하지 않음.

<br>

**추가-01: 캔버스 블록 우선순위 (v0.9)**
- **구현 여부**: ☑ 완성
- **관련 소스**: `view/canvas/InfiniteCanvasPanel.java`, `view/canvas/CanvasBlockComponent.java`
- **설명**: 블록 선택 시 상단 우측에 겹치지 않게 1/2/3 버튼이 뜨고, 1이 가장 위에 그려진다. 같은 우선순위에서는 최근 클릭한 쪽이 위로 올라오며, 값은 `canvas_state.z_order`에 영속화.

<br>

**추가-02: 자동화 연결선 최상위 렌더링**
- **구현 여부**: ☑ 완성
- **관련 소스**: `view/canvas/InfiniteCanvasPanel.java`
- **설명**: 노드 그래프 선은 항상 블록보다 나중에 그려 어떤 우선순위의 블록이 위에 있어도 가려지지 않게 함.

<br>

**통합-01: 전체 프로그램 초기화**
- **구현 여부**: ☑ 완성
- **관련 소스**: `database/DatabaseManager.java` (`wipeAllData`), `view/MainFrame.java`
- **설명**: 상단 "⟲ 초기화" 버튼 → 2단계 확인 후 모든 사용자 테이블 DROP + 스키마 재생성 + 메인 프레임 재시작.