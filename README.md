## 🌿 Branch Convention 

우리 팀은 안정적인 배포와 독립적인 기능 개발을 위해 브랜치를 분리하여 관리합니다.

* 🔵 **`main`** : 항상 배포 가능한 상태를 유지하는 운영 브랜치 (직접 작업 금지, PR 머지만 허용)
* 🟣 **`develop`** : 다음 배포를 준비하는 통합 개발 브랜치 (모든 기능 개발본이 모이는 곳)
* 🟢 **`기능 브랜치`** : 기능/이슈 단위로 `develop`에서 파생하여 개발하는 브랜치
<img width="700" alt="gitflow_no_text_compressed" src="https://github.com/user-attachments/assets/1cdcea08-0adf-4874-8da7-76aa2e1cabc0" />


<br>

## 📌 Branch Naming Convention
* **구조:** `Prefix/#이슈번호-작업내용` (Kebab Case 사용)
* **예시:** `feat/#10-login-api`, `chore/#1-setting-base`

| Prefix | 설명 | 사용 예시 |
| :--- | :--- | :--- |
| `feat` | 새로운 기능 추가 | `feat/#10-login-api` |
| `fix` | 버그 수정 | `fix/#23-header-layout` |
| `docs` | 문서 수정 (README 등) | `docs/#5-update-readme` |
| `style` | 코드 포맷팅 (로직 변경 없음) | `style/#12-format-code` |
| `refactor`| 코드 리팩토링 | `refactor/#30-user-service` |
| `chore` | 설정 파일 변경, 패키지 빌드 등 | `chore/#1-setting-base` |

<br>

## 📝 Commit Convention

| 이모지 | 타입 | 설명 |
|--------|------|------|
| 🎉 `Start` | 프로젝트 초기화 | 프로젝트 생성 및 초기 설정 (`:tada:`) |
| ✨ `Feat` | 새로운 기능 추가 | 새로운 기능 구현 (`:sparkles:`) |
| 🐛 `Fix` | 버그 수정 | 버그 해결 (`:bug:`) |
| 🚑 `Hotfix` | 긴급 버그 수정 | 긴급 수정 |(`:ambulance:`) |
| 🎨 `Design` | UI / CSS 수정 | UI 디자인 변경 (`:art:`) |
| ♻️ `Refactor` | 리팩토링 | 코드 구조 개선 (`:recycle:`) |
| 🔧 `Settings` | 설정 변경 | 환경설정, 설정 파일 수정 (`:wrench:`) |
| 🗃️ `Comment` | 주석 | 필요한 주석 추가/변경 (`:card_file_box:`) |
| ➕ `Dependency/Plugin` | 의존성 추가 | 라이브러리, 플러그인 추가 (`:heavy_plus_sign:`) |
| 📝 `Docs` | 문서 | 문서 수정 (`:memo:`) |
| 🔀 `Merge` | 병합 | 브랜치 병합 (`:twisted_rightwards_arrows:`) |
| 🚀 `Deploy` | 배포 | 배포 관련 작업 (`:rocket:`) |
| 🚚 `Rename` | 이름 변경 | 파일/폴더명 수정 또는 이동 (`:truck:`) |
| 🔥 `Remove` | 삭제 | 파일/코드 삭제 (`:fire:`) |
| ⏪️ `Revert` | 되돌리기 | 이전 버전으로 롤백 (`:rewind:`) |

* **메시지 구조:** `:깃모지: 타입: 작업 내용 (#이슈번호)` 형식으로 작성 
* **예시:** `✨ Feat: 카카오 소셜 로그인 API 추가 (#10)`

<br>

## 🔀 Flow

1. `develop` 브랜치에서 `feature`, `fix`, `chore`, `docs` 브랜치 생성.
2. 작업을 완료하고 커밋 메시지에 맞게 커밋.
3. Pull Request를 생성 / 팀원들의 리뷰.
4. 리뷰가 완료 후 `develop` 브랜치로 병합.
5. 배포 시점에 `develop` 브랜치를 `main` 브랜치로 병합.
6. `main` 브랜치 배포 <br>
#### 예시:
```bash
# 새로운 기능 개발 브랜치 생성
git checkout -b feature/#이슈번호-작업내용

# 작업 후 커밋 & 원격 저장소에 푸시
git add .
git commit -m "✨ Feat: 기능 설명 (#이슈번호)"
git push origin feature/#이슈번호-작업내용

# ➡️ GitHub에서 PR(Pull Request) 생성
#    base: develop ← compare: feature/#이슈번호-작업내용
#    팀원들과 코드 리뷰 후 develop 브랜치로 병합

