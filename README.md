# FinSync - MyData 기반 정기결제 자동화 서비스

## 📋 프로젝트 개요

**신한은행 ICT 인턴십 개인 과제**  
MyData 캘린더 및 소비 패턴 분석을 통한 사용자 정기결제 자동화 서비스

### Getting Started

```
git clone https://github.com/seogwoojin/shinhan-finsync.git
cd shinhan-finsync
docker compose up --build

http://localhost:5173/
```

---

## 1. 🎯 서비스 기획 배경

### 1.1 문제 인식

현대 금융 환경에서 소비자들이 직면한 주요 문제점들:

- **분산된 금융 데이터**: 여러 금융기관에 흩어진 거래 내역으로 인한 통합 관리의 어려움
- **복잡한 정기결제 관리**: 다양한 구독 서비스와 정기 지출에 대한 체계적 관리 부족
- **소비 패턴 분석의 한계**: 개별 금융기관별 제한된 데이터로 인한 종합적 분석 불가
- **수동적 금융 관리**: 사용자가 직접 각 서비스를 확인해야 하는 번거로움
  
### 1.2 서비스 목표

**"모든 금융 거래를 한눈에, 정기결제를 똑똑하게"**

- **통합 관리**: 여러 금융기관의 거래 내역을 하나의 플랫폼에서 통합 관리
- **패턴 분석**: 알고리즘 기반 소비 패턴 분석을 통한 개인 맞춤형 인사이트 제공
- **자동화**: 정기결제 스케줄링 및 자동 이체 실행으로 편의성 극대화
- **유저 캘린더에 스며들기**: 유저가 메인으로 사용하는 캘린더에 정기결제 데이터 삽입, 딥링크를 활용하여 신한은행 앱 내의 이체 내역, 혹은 이체 페이지로 다이렉트 이동 가능 

### 1.3 타겟 사용자

- **Primary**: 다양한 금융 서비스를 이용하는 2030 디지털 네이티브
- **Secondary**: 복잡한 가계부 관리에 어려움을 겪는 일반 소비자
- **Tertiary**: 체계적인 자산 관리를 원하는 금융 초보자

---

## 🎬 2. 시연 영상

### 2.1 시연 시나리오

**🗓️ 통합 캘린더 뷰**


https://github.com/user-attachments/assets/7bcc0457-607f-4481-9c74-d943c07880a9

```
- 유저 이름 입력
- 일별 수입/지출 요약 정보 확인
```

https://github.com/user-attachments/assets/1c5b00db-4c40-4914-b0ad-c4284832123f

```
- 월별 거래 내역을 캘린더 형태로 직관적 표시
- 특정 날짜 클릭 시 상세 거래 내역 조회
```

**📊 마이데이터 연동 유저 정기 소비 패턴 분석 대시보드**


https://github.com/user-attachments/assets/c4968b65-4680-4f3d-b1c6-e7b3cd350ee5


```
- 카테고리별 지출 비율
- 최근 3개월 소비 패턴 기반 미래 예측
- 내용 유사도, 비용 유사도, 일정 유사도 기반 검증
```


https://github.com/user-attachments/assets/8746aca9-8db8-4ddc-b36a-55a34128ee08

```
- 마이데이터에 수집되지 않은 개인 소비 내역 추가 가능
- 데이터 추가 이후, 유사도에 추가된 내용 반양되어 측정 ( 유사도 62% -> 85% )
```

**🔄 정기결제 관리 시스템**

https://github.com/user-attachments/assets/626bb1fb-954b-4333-844e-fbc37b8648b3

```
- 자동 탐지된 정기결제 예상 내역 리스트
- 예상 정기결제 중 원하는 정기 결제만 등록하여 신한은행 자동 이체 서비스에 등록 가능
- 등록한 자동 이체 내역 확인 가능
```


https://github.com/user-attachments/assets/51db8e41-8bf6-4727-ad30-8d20b6e0fc8d

```
- 애플 캘린더와 연동하여 내가 사용하는 캘린더에 해당 정기 결제를 미리 저장 가능 (단 해당 기능은 도커로는 불가능)
- 추가적인 캘린더 확장(구글, TimeBlocks 등등) 및 신한은행 딥링크를 통해 MAU 확장 가능
```

### 2.2 시연 환경 구성

#### 2.2.1 Demo 데이터 구성

- **실제와 유사한 거래 패턴**: 일반적인 직장인의 3개월 거래 내역
- **다양한 금융기관**: 신한은행, 국민은행, 신한카드, 신한투자증권
- **풍부한 카테고리**: 식비, 교통비, 쇼핑, 구독 서비스 등 15개 카테고리


---

## 3. 🛠️ 사용한 기술

### 3.1 시스템 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │    Backend      │    │   External APIs │
│                 │    │                 │    │                 │
│  React + TS     │◄──►│ Spring WebFlux  │◄──►│  금융기관 APIs    │
│  Tailwind CSS   │    │     Kotlin      │    │                 │
│                 │    │                 │    │  • 신한은행       │
└─────────────────┘    └─────────────────┘    │  • 국민은행       │
                                │             │  • 신한카드       │
                                ▼             │  • 신한투자증권    │
                       ┌─────────────────┐    └─────────────────┘
                       │    Database     │
                       │                 │
                       │   H2 + JPA      │
                       │                 │
                       └─────────────────┘
```

### 3.2 Backend 기술 스택

#### 3.2.1 Core Framework

- **Spring Boot 3**: 최신 Spring 생태계의 안정성과 성능
- **Spring WebFlux**: 비동기 논블로킹 I/O로 높은 처리량 구현
- **Kotlin**:
    - Java 대비 간결하고 안전한 코드
    - Null Safety로 런타임 에러 방지
    - 함수형 프로그래밍 지원

#### 3.2.2 외부 API 통합

- **WebClient**:
    - 비동기 HTTP 클라이언트
    - Reactive Stream 지원으로 효율적인 API 호출

```kotlin
fun getTransactions(): Mono<List<TransactionInfo>> {
    return webClient.get()
        .uri("$baseUrl/v1/bank/transactions")
        .retrieve()
        .bodyToMono(object : ParameterizedTypeReference<MyDataResponse<List<TransactionInfo>>>() {})
        .map { response -> response.data ?: emptyList() }
}
```

#### 3.2.3 동시성 처리

- **Reactor**:
    - 병렬 API 호출로 응답 시간 단축
    - 백프레셔 처리로 안정적인 시스템 운영

```kotlin
// 여러 금융기관 API를 병렬로 호출
return Mono.zip(
    cardTransactions,
    shinhanBankTransactions,
    kbBankTransactions,
    investTransactions
).map { tuple ->
    mapOf(
        "shinhan_card" to tuple.t1,
        "shinhan_bank" to tuple.t2,
        "kb_bank" to tuple.t3,
        "shinhan_invest" to tuple.t4
    )
}
```

### 3.3 Frontend 기술 스택

#### 3.3.1 Core Framework

- **React 18**:
    - 컴포넌트 기반 아키텍처
    - Hooks를 활용한 상태 관리
    - Virtual DOM으로 효율적인 렌더링

#### 3.3.2 언어 및 개발 도구

- **TypeScript**:
    - 정적 타입 검사로 런타임 에러 방지
    - IDE 지원 강화로 개발 생산성 향상

```typescript
interface Transaction {
  id: string;
  date: string;
  amount: number;
  description: string;
  type: 'income' | 'expense';
}
```

## 4. 🎯 기대효과

### 4.1 사용자 관점 효과

#### 4.1.1 편의성 향상

- **통합 대시보드**:
    - 여러 금융기관 정보를 한 화면에서 확인
    - 앱 전환 없이 모든 거래 내역 조회 가능

- **자동화된 관리**:
    - 정기결제 일정 자동 추적
    - 정기결제를 자동 이체와 연결하여 등록
    - 내가 사용하는 나만의 캘린더에 정기 결제 내용을 등록 가능

### 4.2 금융기관 관점 효과

#### 4.2.1 고객 접점 확대

- **디지털 채널 강화**:
    - 신한은행 고객의 디지털 서비스 이용률 증가
    - 타 금융기관 고객의 신한은행 서비스 유입 기대
    - 캘린더의 링크를 활용해 유저 MAU 증가
    - 자동 이체 등록을 통한 신한은행 서비스 이용 유도

### 4.3 시장 및 산업 효과

#### 4.3.1 금융 생태계 발전

- **상호 운용성 증대**:
    - 금융기관 간 데이터 연동 표준화 기여
    - API 기반 개방형 금융 생태계 구축
    - 핀테크 스타트업과의 협력 모델 제시


## 📚 참고 자료

### 기술 문서

- [Spring WebFlux Reference](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [React 18 Documentation](https://react.dev/)

---
