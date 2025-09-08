# 📊 WOS Article Filter API

Spring Boot + MongoDB (Docker) ile **Web of Science** JSON verilerini filtreleyen ve raporlayan REST API.

- **Filtreler:** subject (kategori), ascatype (extended/traditional), yıl, yazar sayısı (lt/gte)
- **Raporlar:** subject dağılımı, ascatype kırılımı, yıllara göre histogram, yazar sayısına göre konu dağılımı
- **Çıktı:** JSON veya CSV (`?format=csv`)
- **Doğrulama:** MongoDB shell ile sorguları birebir çalıştırıp sonucu görme

---

## 🧰 Gerekli Araçlar

- **Java 21+** (Temurin önerilir)
- **Maven 3.9+**
- **Docker / Docker Compose**
- **VS Code (opsiyonel)** + Java uzantıları (debug için)

Java sürümünü kontrol:
```bash
java -version
mvn -v


⸻

🚀 Hızlı Başlangıç

1) Depoyu klonla

git clone https://github.com/emresde/ArticleFilter.git
cd ArticleFilter

2) MongoDB’yi Docker ile başlat

docker compose up -d

	•	Container adı: mongo
	•	Veritabanı: appdb
	•	Koleksiyon: data
	•	/import klasörü, konteynere volume olarak bağlıdır.

3) JSON verisini içeri aktar

bigdata.json dosyanı proje kökündeki import/ klasörüne koy ve:

docker exec -it mongo mongoimport \
  --db appdb \
  --collection data \
  --file /import/bigdata.json \
  --jsonArray

Not: Aynı kayıtlardan varsa bazıları “failed” görünebilir; bu normaldir.

4) Önerilen indeksler (performans)

docker exec -it mongo mongosh appdb --eval '
db.data.createIndex({"Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.content":1});
db.data.createIndex({"Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.ascatype":1});
db.data.createIndex({"Data.Records.records.REC.static_data.summary.pub_info.pubyear":1});
'

5) Uygulamayı çalıştır

mvn clean spring-boot:run

	•	Uygulama: http://localhost:8080

⸻

📚 API Dökümantasyonu

Tüm uçlar JSON döndürür; ?format=csv eklenirse CSV döner.

1) Tüm kayıtlar

GET /records

2) Tek subjectContent için toplam

GET /api/stats/subject-content?subjectContent=Food
GET /api/stats/subject-content?subjectContent=Food&format=csv

JSON örnek:

{ "total": 1234 }

3) Tüm subject’ler ve sayıları (opsiyonel ascatype)

GET /api/stats/subjects
GET /api/stats/subjects?ascatype=extended
GET /api/stats/subjects?format=csv

4) Yıllara göre histogram (opsiyonel subjectContent)

GET /api/stats/by-year?subjectContent=Food
GET /api/stats/by-year?subjectContent=Food&format=csv

5) Ascatype toplamları (extended/traditional)

GET /api/stats/ascatype
GET /api/stats/ascatype?format=csv

6) Belirli subject’in ascatype kırılımı

GET /api/stats/subject/count-by-type?subject=Food
GET /api/stats/subject/count-by-type?subject=Food&format=csv

7) Yazar sayısı ile subject+ascatype dağılımı

GET /api/stats/authors/by-subject?ascatype=extended&lt=10&subjectContent=Engineering
GET /api/stats/authors/by-subject?ascatype=extended&lt=10&subjectContent=Engineering&exact=true
GET /api/stats/authors/by-subject?ascatype=extended&lt=10&subjectContent=Engineering&exact=true&format=csv

exact=true → subject.content için tam eşleşme (regex yerine eşitlik)

8) Yazar sayısına göre toplam

GET /api/stats/authors/count?ascatype=extended&lt=5&subjectContent=Food
GET /api/stats/authors/count?ascatype=extended&lt=5&subjectContent=Food&format=csv

Parametreler:
	•	subjectContent: subject.content içinde arama ifadesi (regex; exact=true ile tam eşleşme)
	•	ascatype: extended / traditional
	•	lt: yazar sayısı < lt
	•	gte: yazar sayısı >= gte
	•	exact: true → tam eşleşme; false/yok → regex
	•	format: csv → CSV çıktısı (varsayılan JSON)

⸻

🐚 MongoDB Shell (mongosh) — Doğrulama & Sorgular

0) Shell’e gir

docker exec -it mongo mongosh

veya doğrudan:

docker exec -it mongo mongosh appdb

1) Temel komutlar

show dbs
use appdb
show collections
db.data.countDocuments()
db.data.findOne()

2) Subject içinde “Food” geçen kayıt sayısı (regex)

db.data.countDocuments({
  "Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.content":
  { $regex: "Food", $options: "i" }
})

3) Tüm subject’lerin sayısı (opsiyonel ascatype filtresi)

db.data.aggregate([
  {$unwind:"$Data.Records.records.REC"},
  {$unwind:"$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects"},
  {$unwind:"$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject"},
  // ascatype filtresi istersek (ör. extended):
  // {$match: {"Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.ascatype": /extended/i}},
  {$group:{
    _id:"$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.content",
    count:{$sum:1}
  }},
  {$sort:{count:-1}}
]).toArray()

4) Yıllara göre histogram (opsiyonel subjectContent)

db.data.aggregate([
  {$unwind:"$Data.Records.records.REC"},
  {$match:{
    "Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.content": /Food/i
  }},
  {$group:{
    _id:"$Data.Records.records.REC.static_data.summary.pub_info.pubyear",
    count:{$sum:1}
  }},
  {$sort:{_id:1}}
]).toArray()

5) Belirli subject için ascatype kırılımı

db.data.aggregate([
  {$unwind:"$Data.Records.records.REC"},
  {$unwind:"$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects"},
  {$unwind:"$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject"},
  {$match:{
    "Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.content": /Food/i
  }},
  {$group:{
    _id:"$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject.ascatype",
    count:{$sum:1}
  }},
  {$sort:{count:-1}}
]).toArray()

6) Yazar sayısı metrikleri

Aynı kişi birden çok adreste görünse bile, kişi kimliği PreferredRID (yoksa full_name) ile distinct sayılır.

a) Yazarı < 10 olan makalelerin toplamı (opsiyonel subject/ascatype)

db.data.aggregate([
  {$unwind:"$Data.Records.records.REC"},
  // Makale seviyesinde optional subject/ascatype (elemMatch):
  {$match:{
    "Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject":
      {$elemMatch: {
        // content: /Engineering/i,          // subjectContent için aç (opsiyonel)
        // ascatype: /extended/i             // ascatype için aç (opsiyonel)
      }}
  }},
  // address_name içindeki yazar rolleri
  {$unwind:{
    path:"$Data.Records.records.REC.static_data.fullrecord_metadata.addresses.address_name",
    preserveNullAndEmptyArrays:false
  }},
  {$match:{
    "Data.Records.records.REC.static_data.fullrecord_metadata.addresses.address_name.names.name.role":"author"
  }},
  // Makale UID'e göre unique yazar seti
  {$group:{
    _id:"$Data.Records.records.REC.UID",
    authorsSet: {$addToSet: {
      $ifNull: [
        "$Data.Records.records.REC.static_data.fullrecord_metadata.addresses.address_name.names.name.data-item-ids.data-item-id.content",
        "$Data.Records.records.REC.static_data.fullrecord_metadata.addresses.address_name.names.name.full_name"
      ]
    }},
    subjects: {$first:"$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject"}
  }},
  {$addFields:{ authorCount:{$size:"$authorsSet"} }},
  {$match:{ authorCount: { $lt: 10 } }},    //  <-- eşik buradan değiştirilebilir
  {$count:"total"}
]).toArray()

b) Yazarı < 10 olanların subject dağılımı (exact=true davranışı)

db.data.aggregate([
  {$unwind:"$Data.Records.records.REC"},
  {$match:{
    "Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject":
      {$elemMatch:{
        content: "Engineering",           // exact match
        ascatype: /extended/i             // (opsiyonel)
      }}
  }},
  {$unwind:{
    path:"$Data.Records.records.REC.static_data.fullrecord_metadata.addresses.address_name",
    preserveNullAndEmptyArrays:false
  }},
  {$match:{
    "Data.Records.records.REC.static_data.fullrecord_metadata.addresses.address_name.names.name.role":"author"
  }},
  {$group:{
    _id:"$Data.Records.records.REC.UID",
    authorsSet: {$addToSet: {
      $ifNull: [
        "$Data.Records.records.REC.static_data.fullrecord_metadata.addresses.address_name.names.name.data-item-ids.data-item-id.content",
        "$Data.Records.records.REC.static_data.fullrecord_metadata.addresses.address_name.names.name.full_name"
      ]
    }},
    subjects: {$first:"$Data.Records.records.REC.static_data.fullrecord_metadata.category_info.subjects.subject"}
  }},
  {$addFields:{ authorCount:{$size:"$authorsSet"} }},
  {$match:{ authorCount: { $lt: 10 } }},

  // sadece Engineering (ve varsa ascatype) eşleşen subject elemanlarını say
  {$unwind:"$subjects"},
  {$match:{
    "subjects.content": "Engineering",     // exact=true davranışı
    "subjects.ascatype": /extended/i       // (opsiyonel)
  }},
  {$group:{ _id:{subject:"$subjects.content", ascatype:"$subjects.ascatype"}, count:{$sum:1} }},
  {$sort:{count:-1}}
]).toArray()

exact=false (varsayılan) gibi davranmak için "subjects.content": /Engineering/i yaz.

⸻

🧪 CSV çıktısını API ile alma

Her uçta ?format=csv parametresini ekle:

/api/stats/subjects?ascatype=extended&format=csv
/api/stats/by-year?subjectContent=Food&format=csv
/api/stats/authors/by-subject?ascatype=extended&lt=2&subjectContent=Engineering&exact=true&format=csv
/api/stats/authors/count?lt=10&format=csv


⸻

🐞 Debug / Geliştirme İpuçları (VS Code)
	1.	Java Extensions Pack + Spring Boot Dashboard yüklü olsun.
	2.	DataApplication üzerinde Run/Debug çalıştır.
	3.	web/ altındaki Controller metodlarına breakpoint koy.
	4.	Tarayıcıdan istek at → VS Code’da breakpoint’e düşer, değişkenleri incele.
	5.	Mongo query’lerini loglamak için (isteğe bağlı) application.yml’a:

logging:
  level:
    org.springframework.data.mongodb.core.MongoTemplate: DEBUG



⸻

🛠️ Sorun Giderme
	•	Port/isim çakışması:
Error response from daemon: Conflict. The container name "/mongo" is already in use...

docker rm -f mongo
docker compose up -d


	•	mongoimport hatası (mongosh içinde çalıştırma):
mongoimport shell içinde değil, host’tan docker exec ile çalışır (yukarıdaki komutu kullan).
	•	Spring 500 / Whitelabel Error:
	•	application.yml’da aynı key iki kere olabilir (örn. logging: iki defa). Tekilleştir.
	•	JDK uyumsuzluğu: Java 21 ile derle/çalıştır.
	•	Veriyi sıfırlamak (isteğe bağlı):

docker exec -it mongo mongosh appdb --eval 'db.data.drop()'
# sonra tekrar mongoimport



⸻

🔗 Proje Yapısı (kısa)

src/main/java/com/example/wos
├── dto/              # CountDTO, SubjectCountDTO, SubjectTypeCountDTO, YearCountDTO
├── model/            # WosData
├── repo/             # WosRepository (repository @Query örnekleri)
├── service/          # StatsService, WosRepoService (aggregation pipeline mantığı)
└── web/              # StatsController, RepoController, WosController


⸻