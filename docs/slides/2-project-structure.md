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

# 5. 디렉터리 구조

📎 [디렉터리 구조 관련 문서 바로가기](https://github.com/noctis-03/FinalJavaProject/blob/master/docs/1-requirement-analysis.md)

---

<style scoped>
pre {
  font-size: 0.55rem;
  line-height: 1.2;
}
</style>

## 디렉터리 구조

```
com.blople/
├── App.java
├── common/           # Constants, EventBus, AutoSaveManager, FileUtil
├── model/
│   ├── block/        # Block, ScheduleBlock, PresetBlock 등 
│   ├── calendar/     # CalendarModel, WeekViewModel, MonthViewModel, TimeSlot
│   ├── subject/      # Subject, Assignment, Semester, Timetable
│   ├── automation/   # AutomationRule, AutomationCommand, Slot 계열, ExecutionLog
│   └── enums/        # BlockType, ViewMode, Category 등
├── service/          # CalendarService, BlockService 등 
├── view/
│   ├── calendar/     # 캘린더 페이지 UI 
│   ├── subject/      # 과목 페이지 UI 
│   ├── canvas/       # 무한 캔버스 UI 
│   ├── dialog/       # 공통 다이얼로그 
│   └── component/    # 재사용 컴포넌트 
├── controller/       #  컨트롤러
├── dnd/              # DnD 핸들러
└── database/
    ├── dao/          # DAO
    └── mapper/       # Mapper
```

---

<!-- _class: lead -->
<!-- _paginate: false -->

# 감사합니다
