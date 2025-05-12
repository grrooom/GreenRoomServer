# 📖 Greenroom (Plant Care App)
<img width="200" alt="Image" src="https://github.com/user-attachments/assets/8cbe3653-670a-455b-83b1-0c58b45180e6" />
<img src="https://github.com/user-attachments/assets/8d247827-724c-4527-8ccc-da3ad7f2e7dc" width="200"/>

## 소개 (Introduction)
**Greenroom**은 식물을 키우는 모든 순간을 기록하고, 작은 성장을 함께 축적해 나가는 식물 관리 플랫폼입니다. 초보자부터 숙련자까지, 누구나 자신만의 방식으로 식물과 함께 성장하는 즐거움을 경험할 수 있도록, **Greenroom**은 다양한 기능과 정보를 제공합니다.

## 주요 기능 (Features)
### 회원가입 및 인증/인가
- 어플리케이션 딥링크를 활용한 이메일 본인 인증
- JWT 토큰 + RTR 방식 인증/인가 처리
### 식물 캐릭터 관리
- 사용자가 키우는 식물을 캐릭터로 등록
- 다양한 아이템으로 캐릭터와 공간 꾸미기
### 식물 관리 및 알림
- 키우는 식물 종에 대한 키우기 정보 제공
- 물주기, 가지치기, 분갈이 등의 주기를 등록
- 주기가 도래하면 푸시 알림 전송
- 식물 관리 활동 완료 시 씨앗 포인트 획득을 통한 레벨업과 꾸미기 아이템 구매
### 식물 사전 제공
- 식물별 키우기 정보를 조회할 수 있는 식물 사전 기능 지원
  
<br>

## 화면 예시 (Screenshots)

| 메인 화면 | 식물 사전 화면 |
|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/b0d818a7-6c41-45a9-a278-0705512e628a" width="300"/>| <img src="https://github.com/user-attachments/assets/b0faab2c-9335-46d4-b27b-e75170917344" width="300"/> 

<br>

## 기술 스택

**Backend** : Java&Spring Boot,Spring Data Jpa, Mysql,Firebase 

**Documentation** : Restdocs-api-spec : Restdcos + Swagger

**Infrastructure** : AWS (EC2, S3, RDS, Lambda, Cloud Front), Docker, GitHub Actions (CI/CD)

<br>


## 프로젝트 전체 구조 및 클라우드 인프라
 <img src="https://github.com/user-attachments/assets/6f0335b6-f65a-44eb-be11-dd07e46989dc" width="600"/>
 
 - AWS 환경에서 EC2와 RDS를 하나의 VPC로 구성하고, EC2에는 Public IP를 RDS에는 Private IP를 할당
 - Docker와 Github Actions를 활용한 CI/CD 파이프라인 구축
 - Cloud Front와 Lambda@Edge 기반 이미지 썸네일 로딩

<br>

## security 구조
<img src="https://github.com/user-attachments/assets/e32e316b-6cea-483b-9857-4f42f4519952" width="600"/>

- JwtFilterExceptionHandler 내부에서 CustomJwtFilter를 호출
- CustomJwtFilter에서 Exception 발생 시 JwtFilterExceptionHandler에서 API 공통 응답 형태로 예외값 반환
- API 문서 및 에러 코드 문서는 별도의 Longin을 통해 접근 가능하도록 Security Filter를 분리

<br>

## 어플리케이션 딥링크 기반 이메일 인증

<img width="600" alt="Image" src="https://github.com/user-attachments/assets/aac08f6d-a939-40f4-bba2-36f83a774f0a" />

- 이메일 인증 요청 수신 후 사용자 고유 토큰을 생성해 딥링크 쿼리 파라미터로 추가 & 딥링크를 본문에 추가하여 이메일 전송
- 클라이언트에서 토큰을 파싱하여 이메일 검증 요청을 전달 -> 토큰 값을 검증하여 이메일 인증 진행

<br>

## S3 & DB 정합성 문제 처리

<img width="300" alt="Image" src="https://github.com/user-attachments/assets/a31895be-0539-4350-bb02-e62f74fae3b2" />
<sub>이미지 삭제 처리 로직</sub>

<img width="300" alt="Image" src="https://github.com/user-attachments/assets/a52a4909-f0e8-48f8-b177-13a589c0b445" />
<sub>이미지 업로드 처리 로직</sub>

- 이미지 삭제  : 트랜잭션 commit 이후 s3에 이미지 삭제 요청
- 이미지 업로드 : 트랜잭션 내에서 s3에 이미지 업로드 요청 & 트랜잭션 rollback 이후 s3에 올라간 이미지 삭제 요청
