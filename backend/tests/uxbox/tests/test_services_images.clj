(ns uxbox.tests.test-services-images
  (:require
   [clojure.test :as t]
   [datoteka.core :as fs]
   [uxbox.common.uuid :as uuid]
   [uxbox.db :as db]
   [uxbox.services.mutations :as sm]
   [uxbox.services.queries :as sq]
   [uxbox.tests.helpers :as th]
   [uxbox.util.storage :as ust]))

(t/use-fixtures :once th/state-init)
(t/use-fixtures :each th/database-reset)

(t/deftest image-libraries-crud
  (let [id      (uuid/next)
        prof    (th/create-profile db/pool 2)
        team-id (:default-team prof)]

    (t/testing "create library"
      (let [data {::sm/type :create-image-library
                  :name "sample library"
                  :profile-id (:id prof)
                  :team-id team-id
                  :id id}
            out (th/try-on! (sm/handle data))]

        ;; (th/print-result! out)
        (t/is (nil? (:error out)))

        (let [result (:result out)]
          (t/is (= team-id  (:team-id result)))
          (t/is (= (:name data) (:name result))))))

    (t/testing "rename library"
      (let [data {::sm/type :rename-image-library
                  :name "renamed"
                  :profile-id (:id prof)
                  :id id}
            out (th/try-on! (sm/handle data))]

        ;; (th/print-result! out)
        (t/is (nil? (:error out)))

        (let [result (:result out)]
          (t/is (= id (:id result)))
          (t/is (= "renamed" (:name result))))))

    (t/testing "query single library"
      (let [data {::sq/type :image-library
                  :profile-id (:id prof)
                  :id id}
            out (th/try-on! (sq/handle data))]

        ;; (th/print-result! out)
        (t/is (nil? (:error out)))

        (let [result (:result out)]
          (t/is (= id (:id result)))
          (t/is (= "renamed" (:name result))))))

    (t/testing "query libraries"
      (let [data {::sq/type :image-libraries
                  :team-id team-id
                  :profile-id (:id prof)}
            out (th/try-on! (sq/handle data))]

        ;; (th/print-result! out)
        (t/is (nil? (:error out)))

        (let [result (:result out)]
          (t/is (= 1 (count result)))
          (t/is (= id (get-in result [0 :id]))))))

    (t/testing "delete library"
      (let [data {::sm/type :delete-image-library
                  :profile-id (:id prof)
                  :id id}

            out (th/try-on! (sm/handle data))]

        ;; (th/print-result! out)
        (t/is (nil? (:error out)))
        (t/is (nil? (:result out)))))

    (t/testing "query libraries after delete"
      (let [data {::sq/type :image-libraries
                  :profile-id (:id prof)
                  :team-id team-id}
            out (th/try-on! (sq/handle data))]

        ;; (th/print-result! out)
        (t/is (nil? (:error out)))
        (t/is (= 0 (count (:result out))))))
    ))

(t/deftest images-crud
  (let [prof     (th/create-profile db/pool 1)
        team-id  (:default-team prof)
        lib      (th/create-image-library db/pool team-id 1)
        image-id (uuid/next)]

    (t/testing "upload image to library"
      (let [content {:filename "sample.jpg"
                     :tempfile (th/tempfile "uxbox/tests/_files/sample.jpg")
                     :content-type "image/jpeg"
                     :size 312043}
            data {::sm/type :upload-image
                  :id image-id
                  :profile-id (:id prof)
                  :library-id (:id lib)
                  :name "testfile"
                  :content content}
            out (th/try-on! (sm/handle data))]

        ;; (th/print-result! out)
        (t/is (nil? (:error out)))

        (t/is (= image-id (get-in out [:result :id])))
        (t/is (= "testfile" (get-in out [:result :name])))
        (t/is (= "image/jpeg" (get-in out [:result :mtype])))
        (t/is (= "image/webp" (get-in out [:result :thumb-mtype])))
        (t/is (= 800 (get-in out [:result :width])))
        (t/is (= 800 (get-in out [:result :height])))

        (t/is (string? (get-in out [:result :path])))
        (t/is (string? (get-in out [:result :thumb-path])))
        (t/is (string? (get-in out [:result :uri])))
        (t/is (string? (get-in out [:result :thumb-uri])))
        ))

    (t/testing "list images by library"
      (let [data {::sq/type :images
                  :profile-id (:id prof)
                  :library-id (:id lib)}
            out (th/try-on! (sq/handle data))]
        ;; (th/print-result! out)

        (t/is (= image-id (get-in out [:result 0 :id])))
        (t/is (= "testfile" (get-in out [:result 0 :name])))
        (t/is (= "image/jpeg" (get-in out [:result 0 :mtype])))
        (t/is (= "image/webp" (get-in out [:result 0 :thumb-mtype])))
        (t/is (= 800 (get-in out [:result 0 :width])))
        (t/is (= 800 (get-in out [:result 0 :height])))

        (t/is (string? (get-in out [:result 0 :path])))
        (t/is (string? (get-in out [:result 0 :thumb-path])))
        (t/is (string? (get-in out [:result 0 :uri])))
        (t/is (string? (get-in out [:result 0 :thumb-uri])))))

    (t/testing "single image"
      (let [data {::sq/type :image
                  :profile-id (:id prof)
                  :id image-id}
            out (th/try-on! (sq/handle data))]
        ;; (th/print-result! out)

        (t/is (= image-id (get-in out [:result :id])))
        (t/is (= "testfile" (get-in out [:result :name])))
        (t/is (= "image/jpeg" (get-in out [:result :mtype])))
        (t/is (= "image/webp" (get-in out [:result :thumb-mtype])))
        (t/is (= 800 (get-in out [:result :width])))
        (t/is (= 800 (get-in out [:result :height])))

        (t/is (string? (get-in out [:result :path])))
        (t/is (string? (get-in out [:result :thumb-path])))
        (t/is (string? (get-in out [:result :uri])))
        (t/is (string? (get-in out [:result :thumb-uri])))))

    (t/testing "delete images"
      (let [data {::sm/type :delete-image
                  :profile-id (:id prof)
                  :id image-id}
            out (th/try-on! (sm/handle data))]

        ;; (th/print-result! out)
        (t/is (nil? (:error out)))
        (t/is (nil? (:result out)))))

    (t/testing "query image after delete"
      (let [data {::sq/type :image
                  :profile-id (:id prof)
                  :id image-id}
            out (th/try-on! (sq/handle data))]

        ;; (th/print-result! out)
        (let [error (:error out)]
          (t/is (th/ex-info? error))
          (t/is (th/ex-of-type? error :service-error)))

        (let [error (ex-cause (:error out))]
          (t/is (th/ex-info? error))
          (t/is (th/ex-of-type? error :not-found)))))

    (t/testing "query images after delete"
      (let [data {::sq/type :images
                  :profile-id (:id prof)
                  :library-id (:id lib)}
            out (th/try-on! (sq/handle data))]
        ;; (th/print-result! out)
        (let [result (:result out)]
          (t/is (= 0 (count result))))))
    ))
