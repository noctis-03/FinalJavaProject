# 자바 프로그래밍 기말 프로젝트

## #2 프로젝트 구조 및 역할 일정 관리 계획 


| 항목 |     내용     |
|:----:|:----------:|
| **과목명** |   자바프로그래밍  |
| **학과** |   정보통신공학과  |
| **학번** |  20220540  |
| **이름** |     김가온    |
| **생성일** | 2026.05.13 |
>  [!TIP]
>**프로젝트 전체 자료 모음 (단계별 AI 사용내역) : https://noctis-03.github.io/FinalJavaProject/**


<br>

---



##  <0. 목차>

- [기능적 요구사항](#1-기능적-요구사항)
    - 요구사항 개요
    - 요구사항 상세
- [비기능적 요구사항](#2-비기능적-요구사항)
    - 요구사항 개요
    - 요구사항 추적 매트릭스
- [시스템 설계](#3-시스템-설계)
    - 화면 설계
    - 전체 프로젝트 개요 다이어그램
    - 데이터베이스 이용 시 필요한 필드 이름 정리
- [깃허브 이슈 관리](#4-깃허브-이슈-관리)
    - 라벨 체계
    - 마일스톤
    - 이슈 등록 목록

<br>

---

## <1. 프로젝트 구조 설계>


### 1.1 프로젝트 디렉터리 구조

<details>
<summary>예비 자바 클래스 다이어그램 (클릭하여 자세히 보기)</summary>

``` 
src/
└── main/
└── java/
└── com/
└── blople/
│├── App.java                          # 메인 진입점
│├── common/                           # 공통 유틸·상수
│   ├── Constants.java                # 전역 상수 (색상, 크기 등)
│   ├── EventBus.java                 # 컴포넌트 간 이벤트 통신
│   ├── AutoSaveManager.java          # 자동 저장 관리
│   └── FileUtil.java                 # 파일 I/O 유틸
│├── model/                            # 데이터 모델 계층
│   ├── block/
│   │   ├── Block.java                # 블록 추상 기본 클래스
│   │   ├── ScheduleBlock.java        # 일정 블록 (FR-01)
│   │   ├── PresetBlock.java          # 미리블록 템플릿 (FR-02)
│   │   ├── PresetBlockInstance.java   # 미리블록 인스턴스
│   │   ├── AssignmentBlock.java      # 과제블록 (FR-04)
│   │   ├── FileBlock.java            # 파일블록 (FR-05)
│   │   ├── TextBlock.java            # 텍스트블록 (FR-06)
│   │   ├── LinkBlock.java            # 링크블록 (FR-07)
│   │   └── AutomationBlock.java      # 자동화블록 (FR-13)
│   │
│   ├── calendar/
│   │   ├── CalendarModel.java        # 캘린더 데이터 모델
│   │   ├── WeekViewModel.java        # 주간 뷰 모델
│   │   ├── MonthViewModel.java       # 월간 뷰 모델
│   │   └── TimeSlot.java             # 시간대 슬롯
│   │
│   ├── subject/
│   │   ├── Subject.java              # 과목 모델 (FR-03)
│   │   ├── Assignment.java           # 과제 데이터 (FR-10)
│   │   ├── Semester.java             # 학기 모델
│   │   └── Timetable.java            # 시간표 모델 (FR-09)
│   │
│   ├── automation/
│   │   ├── AutomationRule.java           # 자동화 규칙 (FR-12)
│   │   ├── AutomationCommand.java        # 명령문 템플릿 (일정 등록 블록 등)
│   │   ├── CommandSlot.java              # 슬롯 (빈칸) 공통
│   │   ├── SlotNextWeek.java             # 슬롯 값: 다음 주
│   │   ├── SlotDayOfWeek.java            # 슬롯 값: 요일 선택
│   │   ├── SlotPresetBlock.java          # 슬롯 값: 미리블록 선택
│   │   ├── SlotOpenBlocks.java           # 슬롯 값: 블록 동시 실행
│   │   └── ExecutionLog.java             # 실행 이력
│   │
│   └── enums/
│       ├── BlockType.java
│       ├── AssignmentStatus.java
│       ├── ViewMode.java
│       ├── Category.java
│       ├── CommandType.java              # 명령 유형 (REGISTER_SCHEDULE, OPEN_BLOCKS)
│       └── SlotType.java                 # 슬롯 유형 (NEXT_WEEK, DAY_OF_WEEK, PRESET_BLOCK)
│├── service/                          # 비즈니스 로직 계층
│   ├── CalendarService.java          # 캘린더 CRUD, 충돌 검사
│   ├── BlockService.java             # 블록 생성·삭제·수정
│   ├── SubjectService.java           # 과목 관리
│   ├── AssignmentService.java        # 과제 관리, 마감일 연동
│   ├── TimetableService.java         # 시간표 → 캘린더 반복블록 생성
│   ├── AutomationService.java        # 자동화 실행 엔진
│   ├── FileBlockService.java         # 파일 관리, 열기
│   ├── DragDropService.java          # 드래그앤드롭 유효성 검증
│   └── WeatherService.java           # 날씨 (추후 구현 예약)
│├── view/                             # UI 계층 (Swing)
│   ├── MainFrame.java                # 메인 윈도우 프레임
│   ├── NavigationPanel.java          # 상단 탭 (캘린더/과목 전환)
│   │
│   ├── calendar/                     # 캘린더 페이지 UI
│   │   ├── CalendarPagePanel.java    # 캘린더 페이지 전체 레이아웃
│   │   ├── CalendarGridPanel.java    # 캘린더 그리드 (주간/월간)
│   │   ├── WeekViewPanel.java        # 주간 뷰 렌더링
│   │   ├── MonthViewPanel.java       # 월간 뷰 렌더링
│   │   ├── PresetBlockPanel.java     # 미리블록 목록 패널 (좌측)
│   │   ├── BlockRenderer.java        # 블록 시각적 렌더링
│   │   ├── AutomationPanel.java      # 자동화 창 (FR-12)
│   │   └── WeatherWidget.java        # 날씨 위젯 (우측 상단)
│   │
│   ├── subject/                      # 과목 페이지 UI
│   │   ├── SubjectPagePanel.java     # 과목 페이지 전체 레이아웃
│   │   ├── SubjectListPanel.java     # 좌측 과목 목록
│   │   ├── SubjectDetailPanel.java   # 우측 영역 (캔버스 내장)
│   │   ├── FileBlockListPanel.java   # 파일블록 정렬·필터
│   │   ├── AssignmentInputDialog.java # 과제 입력 폼 (FR-10)
│   │   ├── TimetableInputDialog.java # 시간표 입력 폼 (FR-09)
│   │   └── SubjectSortPanel.java     # 정렬 기능 패널
│   │
│   ├── canvas/                       # 무한 캔버스 보드 UI
│   │   ├── InfiniteCanvasPanel.java  # 무한 캔버스 메인 (FR-08)
│   │   ├── CanvasViewport.java       # 줌·스크롤 관리
│   │   ├── GridRenderer.java         # 격자 무늬 렌더링
│   │   ├── CanvasBlockComponent.java # 캔버스 위 블록 컴포넌트
│   │   ├── TextBlockComponent.java   # 텍스트블록 UI (FR-06)
│   │   ├── LinkBlockComponent.java   # 링크블록 UI (FR-07)
│   │   └── FileBlockComponent.java   # 파일블록 UI (캔버스용)
│   │
│   ├── dialog/                       # 공통 다이얼로그
│   │   ├── BlockEditDialog.java      # 블록 편집 다이얼로그
│   │   ├── PresetBlockCreateDialog.java # 미리블록 생성
│   │   ├── ConfirmDeleteDialog.java  # 삭제 확인
│   │   └── ConflictWarningDialog.java # 충돌 경고
│   │
│   └── component/                    # 재사용 가능한 UI 컴포넌트
│       ├── ColorPicker.java          # 색상 선택기
│       ├── TagChip.java              # 태그 칩 UI
│       ├── StatusBadge.java          # 상태 배지
│       └── DraggableComponent.java   # 드래그 가능 컴포넌트 기반
│├── controller/                       # 컨트롤러 계층
│   ├── CalendarController.java       # 캘린더 이벤트 처리
│   ├── SubjectController.java        # 과목 페이지 이벤트 처리
│   ├── CanvasController.java         # 캔버스 이벤트 처리
│   ├── DragDropController.java       # 드래그앤드롭 이벤트 (FR-11)
│   ├── AutomationController.java     # 자동화 조작 이벤트
│   └── TimetableController.java      # 시간표 이벤트 처리
│├── dnd/                              # 드래그앤드롭 전담 시스템
│   ├── BlockTransferable.java        # Transferable 구현
│   ├── BlockDragSource.java          # 드래그 소스 핸들러
│   ├── CalendarDropTarget.java       # 캘린더 드롭 타겟
│   ├── CanvasDropTarget.java         # 캔버스 드롭 타겟
│   └── DragGhostPanel.java           # 드래그 중 반투명 표시
│└── database/                         # 데이터베이스 계층
    ├── DatabaseManager.java          # DB 연결·초기화
    ├── SchemaInitializer.java        # 테이블 생성 DDL 실행
    ├── TransactionManager.java       # 트랜잭션 관리
    │
    ├── dao/                          # Data Access Object
    │   ├── BaseDAO.java              # 공통 CRUD 추상 클래스
    │   ├── BlockDAO.java             # 블록 공통 DAO
    │   ├── ScheduleBlockDAO.java     # 일정 블록 DAO
    │   ├── PresetBlockDAO.java       # 미리블록 DAO
    │   ├── PresetBlockInstanceDAO.java # 미리블록 인스턴스 DAO
    │   ├── AssignmentDAO.java        # 과제 DAO
    │   ├── FileBlockDAO.java         # 파일블록 DAO
    │   ├── TextBlockDAO.java         # 텍스트블록 DAO
    │   ├── LinkBlockDAO.java         # 링크블록 DAO
    │   ├── SubjectDAO.java           # 과목 DAO
    │   ├── SemesterDAO.java          # 학기 DAO
    │   ├── TimetableDAO.java         # 시간표 DAO
    │   ├── TagDAO.java               # 태그 DAO
    │   ├── CanvasStateDAO.java       # 캔버스 상태 DAO
    │   ├── AutomationRuleDAO.java        # 자동화 규칙 DAO
    │   ├── AutomationCommandDAO.java     # 명령문 DAO
    │   ├── CommandSlotDAO.java           # 슬롯 DAO
    │   ├── SlotNextWeekDAO.java          # 슬롯 값: 다음 주 DAO
    │   ├── SlotDayOfWeekDAO.java         # 슬롯 값: 요일 DAO
    │   ├── SlotPresetBlockDAO.java       # 슬롯 값: 미리블록 DAO
    │   ├── SlotOpenBlocksDAO.java        # 슬롯 값: 블록 실행 DAO
    │   └── ExecutionLogDAO.java      # 실행 이력 DAO
    │    
    └── mapper/                       # ResultSet → Model 변환
    ├── BlockMapper.java
    ├── SubjectMapper.java
    ├── AssignmentMapper.java
    ├── TimetableMapper.java
    └── AutomationMapper.java
```
</details>


<br>

---

### 1.2 자바 클래스 다이어그램


```mermaid

classDiagram
direction TB

    %% ============================================================
    %% ENUMS
    %% ============================================================
    class BlockType {
        <<enumeration>>
        SCHEDULE
        PRESET
        ASSIGNMENT
        FILE
        TEXT
        LINK
        AUTOMATION
    }

    class AssignmentStatus {
        <<enumeration>>
        NOT_STARTED
        IN_PROGRESS
        DONE
    }

    class ViewMode {
        <<enumeration>>
        WEEK
        MONTH
    }

    class Category {
        <<enumeration>>
        STUDY
        EXERCISE
        PART_TIME
        CLUB
        ETC
    }

    class CommandType {
        <<enumeration>>
        REGISTER_SCHEDULE
        OPEN_BLOCKS
    }

    class SlotType {
        <<enumeration>>
        NEXT_WEEK
        DAY_OF_WEEK
        PRESET_BLOCK
    }

    %% ============================================================
    %% MODEL - Block 계층 (핵심)
    %% ============================================================
    class Block {
        <<abstract>>
        -String id
        -BlockType blockType
        -String name
        -String color
        -String memo
        -String createdAt
        -String updatedAt
        +getId() String
        +getBlockType() BlockType
        +getName() String
        +setName(String) void
        +getColor() String
        +setColor(String) void
        +getMemo() String
        +setMemo(String) void
    }

    class ScheduleBlock {
        -String startDatetime
        -String endDatetime
        -String subjectId
        -boolean isRecurring
        -String recurrenceRule
        +getStartDatetime() String
        +setStartDatetime(String) void
        +getEndDatetime() String
        +setEndDatetime(String) void
        +isRecurring() boolean
        +getDurationMinutes() int
    }

    class PresetBlock {
        -int durationMinutes
        -Category category
        +getDurationMinutes() int
        +setDurationMinutes(int) void
        +getCategory() Category
        +createInstance(String, String) PresetBlockInstance
    }

    class PresetBlockInstance {
        -String id
        -String presetBlockId
        -String startDatetime
        -String endDatetime
        +getId() String
        +getPresetBlockId() String
        +getStartDatetime() String
        +setStartDatetime(String) void
        +getEndDatetime() String
        +setEndDatetime(String) void
    }

    class AssignmentBlock {
        -String subjectId
        -String deadline
        -String description
        -String scoreWeight
        -String submitMethod
        -AssignmentStatus status
        -String workDate
        -String workStartTime
        -String workEndTime
        -List~AssignmentAttachment~ attachments
        +getDeadline() String
        +setDeadline(String) void
        +getStatus() AssignmentStatus
        +setStatus(AssignmentStatus) void
        +isDeadlineImminent() boolean
        +getAttachments() List~AssignmentAttachment~
        +addAttachment(AssignmentAttachment) void
    }

    class AssignmentAttachment {
        -String id
        -String assignmentId
        -String filePath
        -String fileName
        +getId() String
        +getFilePath() String
        +getFileName() String
    }

    class FileBlock {
        -String subjectId
        -String filePath
        -String fileName
        -String fileType
        -long fileSize
        +getFilePath() String
        +getFileName() String
        +getFileType() String
        +getFileSize() long
        +openFile() void
    }

    class TextBlock {
        -String subjectId
        -String title
        -String content
        -String backgroundColor
        +getTitle() String
        +setTitle(String) void
        +getContent() String
        +setContent(String) void
        +getBackgroundColor() String
        +setBackgroundColor(String) void
    }

    class LinkBlock {
        -String subjectId
        -String url
        -String displayName
        -String linkType
        +getUrl() String
        +setUrl(String) void
        +getDisplayName() String
        +setDisplayName(String) void
        +openInBrowser() void
    }

    class AutomationBlock {
        -String ruleId
        +getRuleId() String
    }

    Block <|-- ScheduleBlock
    Block <|-- PresetBlock
    Block <|-- AssignmentBlock
    Block <|-- FileBlock
    Block <|-- TextBlock
    Block <|-- LinkBlock
    Block <|-- AutomationBlock

    Block --> BlockType
    AssignmentBlock --> AssignmentStatus
    PresetBlock --> Category
    PresetBlock "1" --> "*" PresetBlockInstance : creates
    AssignmentBlock "1" --> "*" AssignmentAttachment : contains

    %% ============================================================
    %% MODEL - Subject / Calendar
    %% ============================================================
    class Semester {
        -String id
        -String name
        -String startDate
        -String endDate
        -boolean isActive
        -String createdAt
        +getId() String
        +getName() String
        +isActive() boolean
        +setActive(boolean) void
    }

    class Subject {
        -String id
        -String semesterId
        -String name
        -String professor
        -String classroom
        -String color
        -int sortOrder
        -String createdAt
        +getId() String
        +getName() String
        +getColor() String
        +setColor(String) void
        +getProfessor() String
        +getClassroom() String
    }

    class Timetable {
        -String id
        -String subjectId
        -int dayOfWeek
        -String startTime
        -String endTime
        +getId() String
        +getDayOfWeek() int
        +getStartTime() String
        +getEndTime() String
    }

    class CalendarModel {
        -ViewMode currentViewMode
        -LocalDate currentDate
        -List~ScheduleBlock~ scheduleBlocks
        -List~PresetBlockInstance~ presetInstances
        +getCurrentViewMode() ViewMode
        +setCurrentViewMode(ViewMode) void
        +getCurrentDate() LocalDate
        +setCurrentDate(LocalDate) void
        +getBlocksForDate(LocalDate) List~ScheduleBlock~
        +getBlocksForWeek(LocalDate) List~ScheduleBlock~
    }

    class WeekViewModel {
        -LocalDate weekStart
        -List~TimeSlot~ timeSlots
        +getWeekStart() LocalDate
        +getTimeSlotsForDay(int) List~TimeSlot~
    }

    class MonthViewModel {
        -YearMonth yearMonth
        -Map~LocalDate, List~ScheduleBlock~~ dayBlocks
        +getYearMonth() YearMonth
        +getBlocksForDay(LocalDate) List~ScheduleBlock~
    }

    class TimeSlot {
        -LocalDate date
        -LocalTime startTime
        -LocalTime endTime
        -List~Block~ blocks
        +getDate() LocalDate
        +getStartTime() LocalTime
        +getEndTime() LocalTime
        +getBlocks() List~Block~
        +hasConflict() boolean
    }

    Semester "1" --> "*" Subject : contains
    Subject "1" --> "*" Timetable : has
    CalendarModel --> ViewMode
    CalendarModel --> "*" ScheduleBlock
    CalendarModel --> "*" PresetBlockInstance
    CalendarModel --> WeekViewModel
    CalendarModel --> MonthViewModel
    WeekViewModel --> "*" TimeSlot
    TimeSlot --> "*" Block
    Subject "1" --> "*" AssignmentBlock : has
    Subject "1" --> "*" FileBlock : has

    %% ============================================================
    %% MODEL - Tag
    %% ============================================================
    class Tag {
        -String id
        -String name
        +getId() String
        +getName() String
    }

    Block "*" --> "*" Tag : tagged with

    %% ============================================================
    %% MODEL - Canvas
    %% ============================================================
    class CanvasState {
        -String id
        -String subjectId
        -String blockId
        -double posX
        -double posY
        -double width
        -double height
        -int zOrder
        +getPosX() double
        +getPosY() double
        +getWidth() double
        +getHeight() double
        +getZOrder() int
        +setPosition(double, double) void
        +setSize(double, double) void
    }

    CanvasState --> Block
    CanvasState --> Subject

    %% ============================================================
    %% MODEL - Automation 계층
    %% ============================================================
    class AutomationRule {
        -String id
        -String name
        -String context
        -String subjectId
        -boolean isActive
        -String createdAt
        -List~AutomationCommand~ commands
        +getId() String
        +getName() String
        +getContext() String
        +isActive() boolean
        +getCommands() List~AutomationCommand~
        +addCommand(AutomationCommand) void
    }

    class AutomationCommand {
        -String id
        -String ruleId
        -CommandType commandType
        -int sortOrder
        -List~CommandSlot~ slots
        +getId() String
        +getCommandType() CommandType
        +getSlots() List~CommandSlot~
        +addSlot(CommandSlot) void
    }

    class CommandSlot {
        -String id
        -String commandId
        -SlotType slotType
        -int slotOrder
        +getId() String
        +getSlotType() SlotType
        +getSlotOrder() int
    }

    class SlotNextWeek {
        -String slotId
        -int weekOffset
        +getSlotId() String
        +getWeekOffset() int
        +calculateTargetWeekStart() LocalDate
    }

    class SlotDayOfWeek {
        -String id
        -String slotId
        -int dayOfWeek
        +getDayOfWeek() int
    }

    class SlotPresetBlock {
        -String slotId
        -String presetBlockId
        +getPresetBlockId() String
    }

    class SlotOpenBlocks {
        -String id
        -String slotId
        -String blockId
        +getBlockId() String
    }

    class ExecutionLog {
        -String id
        -String ruleId
        -String executedAt
        -String status
        -String resultMessage
        +getId() String
        +getStatus() String
        +getResultMessage() String
    }

    AutomationRule "1" --> "*" AutomationCommand : contains
    AutomationCommand --> CommandType
    AutomationCommand "1" --> "*" CommandSlot : has
    CommandSlot --> SlotType
    CommandSlot "1" --> "0..1" SlotNextWeek : value
    CommandSlot "1" --> "*" SlotDayOfWeek : value
    CommandSlot "1" --> "0..1" SlotPresetBlock : value
    CommandSlot "1" --> "*" SlotOpenBlocks : value
    SlotPresetBlock --> PresetBlock
    SlotOpenBlocks --> Block
    AutomationRule "1" --> "*" ExecutionLog : logs
    AutomationRule --> Subject

    %% ============================================================
    %% MODEL - SortPreference
    %% ============================================================
    class SortPreference {
        -String id
        -String subjectId
        -String sortField
        -String sortDirection
        +getSortField() String
        +getSortDirection() String
    }

    Subject "1" --> "0..1" SortPreference : has

    %% ============================================================
    %% DATABASE - Manager & Initializer
    %% ============================================================
    class DatabaseManager {
        -Connection connection
        -String dbPath
        +getConnection() Connection
        +initialize() void
        +close() void
    }

    class SchemaInitializer {
        -DatabaseManager dbManager
        +createAllTables() void
        +dropAllTables() void
    }

    class TransactionManager {
        -DatabaseManager dbManager
        +beginTransaction() void
        +commit() void
        +rollback() void
        +executeInTransaction(Runnable) void
    }

    SchemaInitializer --> DatabaseManager
    TransactionManager --> DatabaseManager

    %% ============================================================
    %% DATABASE - DAO 계층
    %% ============================================================
    class BaseDAO~T~ {
        <<abstract>>
        #DatabaseManager dbManager
        +findById(String) T
        +findAll() List~T~
        +insert(T) void
        +update(T) void
        +delete(String) void
    }

    class BlockDAO {
        +findById(String) Block
        +findAll() List~Block~
        +findByType(BlockType) List~Block~
        +insert(Block) void
        +update(Block) void
        +delete(String) void
    }

    class ScheduleBlockDAO {
        +findByDateRange(String, String) List~ScheduleBlock~
        +findBySubjectId(String) List~ScheduleBlock~
        +findConflicts(String, String) List~ScheduleBlock~
        +insert(ScheduleBlock) void
        +update(ScheduleBlock) void
    }

    class PresetBlockDAO {
        +findAll() List~PresetBlock~
        +findByCategory(Category) List~PresetBlock~
        +insert(PresetBlock) void
        +update(PresetBlock) void
    }

    class PresetBlockInstanceDAO {
        +findByPresetBlockId(String) List~PresetBlockInstance~
        +findByDateRange(String, String) List~PresetBlockInstance~
        +insert(PresetBlockInstance) void
        +delete(String) void
    }

    class AssignmentDAO {
        +findBySubjectId(String) List~AssignmentBlock~
        +findByStatus(AssignmentStatus) List~AssignmentBlock~
        +findImminentDeadlines(int) List~AssignmentBlock~
        +insert(AssignmentBlock) void
        +update(AssignmentBlock) void
    }

    class FileBlockDAO {
        +findBySubjectId(String) List~FileBlock~
        +findByTag(String) List~FileBlock~
        +insert(FileBlock) void
        +update(FileBlock) void
    }

    class TextBlockDAO {
        +findBySubjectId(String) List~TextBlock~
        +insert(TextBlock) void
        +update(TextBlock) void
    }

    class LinkBlockDAO {
        +findBySubjectId(String) List~LinkBlock~
        +insert(LinkBlock) void
        +update(LinkBlock) void
    }

    class SubjectDAO {
        +findBySemesterId(String) List~Subject~
        +findById(String) Subject
        +insert(Subject) void
        +update(Subject) void
        +delete(String) void
    }

    class SemesterDAO {
        +findActive() Semester
        +findAll() List~Semester~
        +insert(Semester) void
        +update(Semester) void
    }

    class TimetableDAO {
        +findBySubjectId(String) List~Timetable~
        +findByDay(int) List~Timetable~
        +insert(Timetable) void
        +delete(String) void
    }

    class TagDAO {
        +findByName(String) Tag
        +findAll() List~Tag~
        +findByBlockId(String) List~Tag~
        +insert(Tag) void
        +linkBlockTag(String, String) void
        +unlinkBlockTag(String, String) void
    }

    class CanvasStateDAO {
        +findBySubjectId(String) List~CanvasState~
        +findByBlockId(String) CanvasState
        +insert(CanvasState) void
        +update(CanvasState) void
        +delete(String) void
    }

    class AutomationRuleDAO {
        +findByContext(String) List~AutomationRule~
        +findBySubjectId(String) List~AutomationRule~
        +insert(AutomationRule) void
        +update(AutomationRule) void
        +delete(String) void
    }

    class AutomationCommandDAO {
        +findByRuleId(String) List~AutomationCommand~
        +insert(AutomationCommand) void
        +update(AutomationCommand) void
        +delete(String) void
    }

    class CommandSlotDAO {
        +findByCommandId(String) List~CommandSlot~
        +insert(CommandSlot) void
        +update(CommandSlot) void
        +delete(String) void
    }

    class SlotNextWeekDAO {
        +findBySlotId(String) SlotNextWeek
        +insert(SlotNextWeek) void
        +update(SlotNextWeek) void
    }

    class SlotDayOfWeekDAO {
        +findBySlotId(String) List~SlotDayOfWeek~
        +insert(SlotDayOfWeek) void
        +deleteBySlotId(String) void
    }

    class SlotPresetBlockDAO {
        +findBySlotId(String) SlotPresetBlock
        +insert(SlotPresetBlock) void
        +update(SlotPresetBlock) void
    }

    class SlotOpenBlocksDAO {
        +findBySlotId(String) List~SlotOpenBlocks~
        +insert(SlotOpenBlocks) void
        +deleteBySlotId(String) void
    }

    class ExecutionLogDAO {
        +findByRuleId(String) List~ExecutionLog~
        +insert(ExecutionLog) void
    }

    BaseDAO <|-- BlockDAO
    BaseDAO <|-- ScheduleBlockDAO
    BaseDAO <|-- PresetBlockDAO
    BaseDAO <|-- PresetBlockInstanceDAO
    BaseDAO <|-- AssignmentDAO
    BaseDAO <|-- FileBlockDAO
    BaseDAO <|-- TextBlockDAO
    BaseDAO <|-- LinkBlockDAO
    BaseDAO <|-- SubjectDAO
    BaseDAO <|-- SemesterDAO
    BaseDAO <|-- TimetableDAO
    BaseDAO <|-- TagDAO
    BaseDAO <|-- CanvasStateDAO
    BaseDAO <|-- AutomationRuleDAO
    BaseDAO <|-- AutomationCommandDAO
    BaseDAO <|-- CommandSlotDAO
    BaseDAO <|-- SlotNextWeekDAO
    BaseDAO <|-- SlotDayOfWeekDAO
    BaseDAO <|-- SlotPresetBlockDAO
    BaseDAO <|-- SlotOpenBlocksDAO
    BaseDAO <|-- ExecutionLogDAO

    BaseDAO --> DatabaseManager

    %% ============================================================
    %% DATABASE - Mapper 계층
    %% ============================================================
    class BlockMapper {
        +toBlock(ResultSet) Block
        +toScheduleBlock(ResultSet) ScheduleBlock
        +toPresetBlock(ResultSet) PresetBlock
        +toAssignmentBlock(ResultSet) AssignmentBlock
        +toFileBlock(ResultSet) FileBlock
        +toTextBlock(ResultSet) TextBlock
        +toLinkBlock(ResultSet) LinkBlock
    }

    class SubjectMapper {
        +toSubject(ResultSet) Subject
        +toSemester(ResultSet) Semester
    }

    class AssignmentMapper {
        +toAssignment(ResultSet) AssignmentBlock
        +toAttachment(ResultSet) AssignmentAttachment
    }

    class TimetableMapper {
        +toTimetable(ResultSet) Timetable
    }

    class AutomationMapper {
        +toRule(ResultSet) AutomationRule
        +toCommand(ResultSet) AutomationCommand
        +toSlot(ResultSet) CommandSlot
        +toExecutionLog(ResultSet) ExecutionLog
    }

    BlockDAO --> BlockMapper
    SubjectDAO --> SubjectMapper
    SemesterDAO --> SubjectMapper
    AssignmentDAO --> AssignmentMapper
    TimetableDAO --> TimetableMapper
    AutomationRuleDAO --> AutomationMapper
    AutomationCommandDAO --> AutomationMapper
    CommandSlotDAO --> AutomationMapper
    ExecutionLogDAO --> AutomationMapper

    %% ============================================================
    %% COMMON (유틸리티)
    %% ============================================================
    class Constants {
        +DEFAULT_WINDOW_WIDTH int$
        +DEFAULT_WINDOW_HEIGHT int$
        +GRID_SNAP_SIZE int$
        +AUTO_SAVE_INTERVAL_MS int$
        +MAX_UNDO_COUNT int$
        +DEADLINE_WARNING_DAYS int$
    }

    class EventBus {
        -Map~String, List~Consumer~~ listeners
        +subscribe(String, Consumer) void
        +publish(String, Object) void
        +unsubscribe(String, Consumer) void
    }

    class AutoSaveManager {
        -Timer timer
        -int intervalMs
        +start() void
        +stop() void
        +saveNow() void
    }

    class FileUtil {
        +copyFile(String, String) void$
        +getFileExtension(String) String$
        +isSupported(String) boolean$
        +getFileIcon(String) ImageIcon$
        +openWithSystem(String) void$
    }

    AutoSaveManager --> EventBus

    %% ============================================================
    %% SERVICE 계층
    %% ============================================================
    class CalendarService {
        -ScheduleBlockDAO scheduleBlockDAO
        -PresetBlockInstanceDAO presetInstanceDAO
        -CalendarModel calendarModel
        +getBlocksForWeek(LocalDate) List~ScheduleBlock~
        +getBlocksForMonth(YearMonth) Map
        +createScheduleBlock(ScheduleBlock) void
        +moveBlock(String, String, String) void
        +resizeBlock(String, String) void
        +checkConflicts(String, String) List~ScheduleBlock~
        +switchView(ViewMode) void
    }

    class BlockService {
        -BlockDAO blockDAO
        -TagDAO tagDAO
        +createBlock(Block) void
        +updateBlock(Block) void
        +deleteBlock(String) void
        +addTag(String, String) void
        +removeTag(String, String) void
        +findByType(BlockType) List~Block~
    }

    class SubjectService {
        -SubjectDAO subjectDAO
        -SemesterDAO semesterDAO
        +createSubject(Subject) void
        +updateSubject(Subject) void
        +deleteSubject(String) void
        +getSubjectsBySemester(String) List~Subject~
        +getActiveSemester() Semester
        +createSemester(Semester) void
    }

    class AssignmentService {
        -AssignmentDAO assignmentDAO
        -ScheduleBlockDAO scheduleBlockDAO
        -EventBus eventBus
        +createAssignment(AssignmentBlock) void
        +updateAssignment(AssignmentBlock) void
        +deleteAssignment(String) void
        +updateStatus(String, AssignmentStatus) void
        +getImminentDeadlines(int) List~AssignmentBlock~
        +syncDeadlineToCalendar(AssignmentBlock) void
        +placeWorkSchedule(String, String, String, String) void
    }

    class TimetableService {
        -TimetableDAO timetableDAO
        -SubjectDAO subjectDAO
        -ScheduleBlockDAO scheduleBlockDAO
        -EventBus eventBus
        +addEntry(Timetable) void
        +deleteEntry(String) void
        +getEntriesBySubject(String) List~Timetable~
        +generateRecurringBlocks(String) void
        +checkTimeConflict(Timetable) boolean
    }

    class AutomationService {
        -AutomationRuleDAO ruleDAO
        -AutomationCommandDAO commandDAO
        -CommandSlotDAO slotDAO
        -ExecutionLogDAO logDAO
        -CalendarService calendarService
        -PresetBlockDAO presetBlockDAO
        +createRule(AutomationRule) void
        +executeRule(String) ExecutionLog
        +executeRegisterSchedule(AutomationCommand) void
        +executeOpenBlocks(AutomationCommand) void
        +getExecutionHistory(String) List~ExecutionLog~
    }

    class FileBlockService {
        -FileBlockDAO fileBlockDAO
        -FileUtil fileUtil
        +createFileBlock(FileBlock) void
        +openFile(String) void
        +deleteFileBlock(String) void
        +getFilesBySubject(String) List~FileBlock~
        +getFilesByTag(String) List~FileBlock~
        +validateFilePath(String) boolean
    }

    class DragDropService {
        -CalendarService calendarService
        -BlockService blockService
        +validateDrop(Block, String) boolean
        +executeDrop(Block, String, Object) void
        +getDropFeedback(Block, String) String
    }

    class WeatherService {
        +getTomorrowWeather() String
    }

    CalendarService --> ScheduleBlockDAO
    CalendarService --> PresetBlockInstanceDAO
    CalendarService --> CalendarModel
    BlockService --> BlockDAO
    BlockService --> TagDAO
    SubjectService --> SubjectDAO
    SubjectService --> SemesterDAO
    AssignmentService --> AssignmentDAO
    AssignmentService --> ScheduleBlockDAO
    AssignmentService --> EventBus
    TimetableService --> TimetableDAO
    TimetableService --> SubjectDAO
    TimetableService --> ScheduleBlockDAO
    TimetableService --> EventBus
    AutomationService --> AutomationRuleDAO
    AutomationService --> AutomationCommandDAO
    AutomationService --> CommandSlotDAO
    AutomationService --> ExecutionLogDAO
    AutomationService --> CalendarService
    AutomationService --> PresetBlockDAO
    FileBlockService --> FileBlockDAO
    FileBlockService --> FileUtil
    DragDropService --> CalendarService
    DragDropService --> BlockService

    %% ============================================================
    %% CONTROLLER 계층
    %% ============================================================
    class CalendarController {
        -CalendarService calendarService
        -BlockService blockService
        -EventBus eventBus
        +onViewModeChanged(ViewMode) void
        +onDateNavigated(LocalDate) void
        +onBlockCreated(ScheduleBlock) void
        +onBlockMoved(String, String, String) void
        +onBlockResized(String, String) void
        +onBlockDeleted(String) void
        +onConflictDetected(List~ScheduleBlock~) void
    }

    class SubjectController {
        -SubjectService subjectService
        -AssignmentService assignmentService
        -FileBlockService fileBlockService
        -EventBus eventBus
        +onSubjectSelected(String) void
        +onSubjectCreated(Subject) void
        +onSubjectDeleted(String) void
        +onAssignmentCreated(AssignmentBlock) void
        +onFileBlockCreated(FileBlock) void
        +onSortChanged(String, String) void
    }

    class CanvasController {
        -BlockService blockService
        -FileBlockService fileBlockService
        -EventBus eventBus
        -CanvasStateDAO canvasStateDAO
        +onBlockPlaced(String, double, double) void
        +onBlockMoved(String, double, double) void
        +onBlockResized(String, double, double) void
        +onBlockRemoved(String) void
        +onZoomChanged(double) void
        +onBlockDoubleClicked(String) void
        +saveCanvasState() void
        +loadCanvasState(String) List~CanvasState~
    }

    class DragDropController {
        -DragDropService dragDropService
        -EventBus eventBus
        +onDragStarted(Block, String) void
        +onDragOver(String, Object) void
        +onDropped(Block, String, Object) void
        +onDragCancelled() void
    }

    class AutomationController {
        -AutomationService automationService
        -EventBus eventBus
        +onRuleCreated(AutomationRule) void
        +onSlotFilled(String, Block) void
        +onExecuteClicked(String) void
        +onRuleDeleted(String) void
    }

    class TimetableController {
        -TimetableService timetableService
        -SubjectService subjectService
        -EventBus eventBus
        +onEntryAdded(Timetable) void
        +onEntryDeleted(String) void
        +onSemesterChanged(String) void
        +onConflictDetected(Timetable) void
    }

    CalendarController --> CalendarService
    CalendarController --> BlockService
    CalendarController --> EventBus
    SubjectController --> SubjectService
    SubjectController --> AssignmentService
    SubjectController --> FileBlockService
    SubjectController --> EventBus
    CanvasController --> BlockService
    CanvasController --> FileBlockService
    CanvasController --> CanvasStateDAO
    CanvasController --> EventBus
    DragDropController --> DragDropService
    DragDropController --> EventBus
    AutomationController --> AutomationService
    AutomationController --> EventBus
    TimetableController --> TimetableService
    TimetableController --> SubjectService
    TimetableController --> EventBus

    %% ============================================================
    %% DnD 시스템
    %% ============================================================
    class BlockTransferable {
        -Block block
        -String sourceContext
        +getTransferDataFlavors() DataFlavor[]
        +getTransferData(DataFlavor) Object
        +getBlock() Block
        +getSourceContext() String
    }

    class BlockDragSource {
        -DragDropController controller
        +startDrag(Block, Component) void
        +createDragGhost(Block) DragGhostPanel
    }

    class CalendarDropTarget {
        -DragDropController controller
        -CalendarGridPanel targetPanel
        +drop(DropTargetDropEvent) void
        +dragOver(DropTargetDragEvent) void
        +getTimeSlotAt(Point) TimeSlot
    }

    class CanvasDropTarget {
        -DragDropController controller
        -InfiniteCanvasPanel targetPanel
        +drop(DropTargetDropEvent) void
        +dragOver(DropTargetDragEvent) void
        +getCanvasPositionAt(Point) Point2D
    }

    class DragGhostPanel {
        -Block block
        -float opacity
        +paintComponent(Graphics) void
        +setOpacity(float) void
    }

    BlockDragSource --> DragDropController
    BlockDragSource --> BlockTransferable
    BlockDragSource --> DragGhostPanel
    CalendarDropTarget --> DragDropController
    CanvasDropTarget --> DragDropController
    BlockTransferable --> Block

    %% ============================================================
    %% VIEW - 메인 프레임 & 네비게이션
    %% ============================================================
    class MainFrame {
        -NavigationPanel navigationPanel
        -CalendarPagePanel calendarPage
        -SubjectPagePanel subjectPage
        -EventBus eventBus
        +initialize() void
        +switchPage(String) void
    }

    class NavigationPanel {
        -JButton calendarTab
        -JButton subjectTab
        +setActiveTab(String) void
        +addTabListener(ActionListener) void
    }

    MainFrame --> NavigationPanel
    MainFrame --> EventBus

    %% ============================================================
    %% VIEW - Calendar 페이지
    %% ============================================================
    class CalendarPagePanel {
        -CalendarGridPanel gridPanel
        -PresetBlockPanel presetPanel
        -WeatherWidget weatherWidget
        -AutomationPanel automationPanel
        -CalendarController controller
        +initialize() void
        +refresh() void
    }

    class CalendarGridPanel {
        -WeekViewPanel weekView
        -MonthViewPanel monthView
        -ViewMode currentMode
        +switchView(ViewMode) void
        +refresh() void
    }

    class WeekViewPanel {
        -List~BlockRenderer~ blockRenderers
        +paintComponent(Graphics) void
        +renderTimeGrid() void
        +renderBlocks(List~ScheduleBlock~) void
    }

    class MonthViewPanel {
        -Map~LocalDate, List~BlockRenderer~~ dayRenderers
        +paintComponent(Graphics) void
        +renderMonthGrid() void
        +renderDayBlocks(LocalDate, List~ScheduleBlock~) void
    }

    class PresetBlockPanel {
        -List~PresetBlock~ presets
        +addPresetBlock(PresetBlock) void
        +removePresetBlock(String) void
        +refresh() void
    }

    class BlockRenderer {
        -Block block
        -Rectangle bounds
        +paint(Graphics2D) void
        +contains(Point) boolean
        +getBounds() Rectangle
        +setHighlight(boolean) void
    }

    class AutomationPanel {
        -AutomationController controller
        +showAutomationCanvas() void
        +refresh() void
    }

    class WeatherWidget {
        -WeatherService weatherService
        +refresh() void
        +paintComponent(Graphics) void
    }

    CalendarPagePanel --> CalendarGridPanel
    CalendarPagePanel --> PresetBlockPanel
    CalendarPagePanel --> WeatherWidget
    CalendarPagePanel --> AutomationPanel
    CalendarPagePanel --> CalendarController
    CalendarGridPanel --> WeekViewPanel
    CalendarGridPanel --> MonthViewPanel
    WeekViewPanel --> "*" BlockRenderer
    MonthViewPanel --> "*" BlockRenderer
    AutomationPanel --> AutomationController
    WeatherWidget --> WeatherService
    MainFrame --> CalendarPagePanel

    %% ============================================================
    %% VIEW - Subject 페이지
    %% ============================================================
    class SubjectPagePanel {
        -SubjectListPanel listPanel
        -SubjectDetailPanel detailPanel
        -SubjectController controller
        +initialize() void
        +refresh() void
    }

    class SubjectListPanel {
        -List~Subject~ subjects
        +addSubject(Subject) void
        +removeSubject(String) void
        +setSelectedSubject(String) void
    }

    class SubjectDetailPanel {
        -InfiniteCanvasPanel canvas
        -FileBlockListPanel fileListPanel
        -SubjectSortPanel sortPanel
        +loadSubject(String) void
        +refresh() void
    }

    class FileBlockListPanel {
        -List~FileBlock~ fileBlocks
        +addFileBlock(FileBlock) void
        +filterByTag(String) void
        +sortBy(String, String) void
        +refresh() void
    }

    class AssignmentInputDialog {
        -AssignmentBlock assignment
        -SubjectController controller
        +showDialog() void
        +getResult() AssignmentBlock
        +validateInput() boolean
    }

    class TimetableInputDialog {
        -Timetable entry
        -TimetableController controller
        +showDialog() void
        +getResult() Timetable
        +validateInput() boolean
    }

    class SubjectSortPanel {
        -String currentSortField
        -String currentDirection
        +setSortField(String) void
        +setSortDirection(String) void
        +getSortPreference() SortPreference
    }

    SubjectPagePanel --> SubjectListPanel
    SubjectPagePanel --> SubjectDetailPanel
    SubjectPagePanel --> SubjectController
    SubjectDetailPanel --> FileBlockListPanel
    SubjectDetailPanel --> SubjectSortPanel
    AssignmentInputDialog --> SubjectController
    TimetableInputDialog --> TimetableController
    MainFrame --> SubjectPagePanel

    %% ============================================================
    %% VIEW - Canvas
    %% ============================================================
    class InfiniteCanvasPanel {
        -CanvasViewport viewport
        -GridRenderer gridRenderer
        -List~CanvasBlockComponent~ blockComponents
        -CanvasController controller
        +addBlock(CanvasBlockComponent) void
        +removeBlock(String) void
        +paintComponent(Graphics) void
    }

    class CanvasViewport {
        -double zoomLevel
        -double offsetX
        -double offsetY
        +zoom(double) void
        +pan(double, double) void
        +screenToCanvas(Point) Point2D
        +canvasToScreen(Point2D) Point
        +getZoomLevel() double
    }

    class GridRenderer {
        -int gridSize
        -Color gridColor
        +paint(Graphics2D, CanvasViewport) void
        +snapToGrid(double, double) Point2D
    }

    class CanvasBlockComponent {
        <<abstract>>
        #Block block
        #CanvasState state
        +paint(Graphics2D) void
        +contains(Point) boolean
        +getBlock() Block
        +setPosition(double, double) void
        +setSize(double, double) void
    }

    class TextBlockComponent {
        -TextBlock textBlock
        -boolean editing
        +paintComponent(Graphics2D) void
        +enterEditMode() void
        +exitEditMode() void
    }

    class LinkBlockComponent {
        -LinkBlock linkBlock
        +paintComponent(Graphics2D) void
        +onClick() void
    }

    class FileBlockComponent {
        -FileBlock fileBlock
        +paintComponent(Graphics2D) void
        +onDoubleClick() void
    }

    InfiniteCanvasPanel --> CanvasViewport
    InfiniteCanvasPanel --> GridRenderer
    InfiniteCanvasPanel --> "*" CanvasBlockComponent
    InfiniteCanvasPanel --> CanvasController
    CanvasBlockComponent <|-- TextBlockComponent
    CanvasBlockComponent <|-- LinkBlockComponent
    CanvasBlockComponent <|-- FileBlockComponent
    CanvasBlockComponent --> Block
    CanvasBlockComponent --> CanvasState
    SubjectDetailPanel --> InfiniteCanvasPanel

    %% ============================================================
    %% VIEW - 공통 다이얼로그 & 컴포넌트
    %% ============================================================
    class BlockEditDialog {
        -Block block
        +showDialog() void
        +getResult() Block
    }

    class PresetBlockCreateDialog {
        +showDialog() void
        +getResult() PresetBlock
    }

    class ConfirmDeleteDialog {
        -String message
        +showDialog() boolean
    }

    class ConflictWarningDialog {
        -List~ScheduleBlock~ conflicts
        +showDialog() void
    }

    class ColorPicker {
        -String selectedColor
        +getSelectedColor() String
        +showPicker() void
    }

    class TagChip {
        -Tag tag
        -boolean removable
        +getTag() Tag
        +onClick() void
        +onRemove() void
    }

    class StatusBadge {
        -AssignmentStatus status
        +paintComponent(Graphics) void
        +setStatus(AssignmentStatus) void
    }

    class DraggableComponent {
        <<abstract>>
        #boolean dragging
        #Point dragOffset
        +mousePressed(MouseEvent) void
        +mouseDragged(MouseEvent) void
        +mouseReleased(MouseEvent) void
    }

    StatusBadge --> AssignmentStatus
    TagChip --> Tag
    DraggableComponent <|-- CanvasBlockComponent
    DraggableComponent <|-- BlockRenderer

    %% ============================================================
    %% APP 진입점
    %% ============================================================
    class App {
        +main(String[]) void$
        -initializeDatabase() void
        -initializeUI() void
        -initializeServices() void
        -initializeAutoSave() void
    }

    App --> MainFrame
    App --> DatabaseManager
    App --> EventBus
    App --> AutoSaveManager

```