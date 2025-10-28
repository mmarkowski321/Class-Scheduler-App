// src/pages/app/tutor/TutorOverview.jsx
import { useTranslation } from "react-i18next";
import Upcoming from "../shared/Upcoming";
import CardEmpty from "../shared/CardEmpty";

export default function TutorOverview() {
  const { t } = useTranslation("common");
  const lessons = []; // TODO: fetch

  return (
    <>
      <Upcoming title={t("tutor.overview.upcomingTitle")} items={lessons} />
      {!lessons.length && (
        <CardEmpty text={t("tutor.overview.empty")} />
      )}
    </>
  );
}
