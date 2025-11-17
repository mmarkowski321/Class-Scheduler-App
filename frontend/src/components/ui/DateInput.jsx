import React from "react";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { AdapterDateFns } from "@mui/x-date-pickers/AdapterDateFns";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import { pl, enUS } from "date-fns/locale";
import TextField from "@mui/material/TextField";
import "./DateInput.css";

function DateInput({ label, value, onChange, error, lang, maxDate }) {
  return (
    <div className="field">
      <label htmlFor="birthDate">{label}</label>
      <LocalizationProvider
        dateAdapter={AdapterDateFns}
        adapterLocale={lang === "pl" ? pl : enUS}
      >
        <DatePicker
          className="custom-datepicker"
          value={value ? new Date(value) : null}
          onChange={(newValue) => {
            onChange({
              target: {
                name: "birthDate",
                value: newValue ? newValue.toISOString().split("T")[0] : "",
              },
            });
          }}
          maxDate={maxDate || new Date()}   
          format="dd.MM.yyyy"
          enableAccessibleFieldDOMStructure={false}
          slotProps={{
            textField: {
              fullWidth: true,
              error: !!error,
              helperText: error || "",
              className: "custom-datepicker-field",
              placeholder: lang === "pl" ? "dd.mm.rrrr" : "dd.mm.yyyy",
            },
          }}
          slots={{ textField: TextField }}
        />
      </LocalizationProvider>
    </div>
  );
}



export default DateInput;
