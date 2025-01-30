import React, { useEffect } from 'react';
import { IonButton, IonItem, IonChip, IonLabel } from '@ionic/react';
import {
  BaseInputTemplateProps,
  FormContextType,
  RJSFSchema,
  StrictRJSFSchema,
} from '@rjsf/utils';
import { v4 as uuidv4 } from 'uuid';

export default function IonUuidWidget<
  T = any,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = any
>({
  id,
  name,
  value,
  required,
  disabled,
  readonly,
  onChange,
  onBlur,
  onFocus,
  label,
  hideLabel,
  options,
  schema,
  rawErrors = [],
  registry,
}: BaseInputTemplateProps<T, S, F>) {
  useEffect(() => {
    // Initialize with UUID if no value and not disabled/readonly
    if (!value && !disabled && !readonly) {
      const newUuid = uuidv4();
      onChange(newUuid);
    }
  }, []); // Only run on mount

  const generateNewUuid = () => {
    onChange(uuidv4());
  };

  return (
    <IonItem lines="none" className="ion-margin-vertical">
      <div className="w-full">
        {!hideLabel && label && (
          <IonLabel position="stacked">
            {label}
            {required && <span className="text-danger"> *</span>}
          </IonLabel>
        )}
        <input type="hidden" id={id} name={name} value={value} />
        <IonChip
          id={`${id}-display`}
          disabled={disabled || readonly}
          className="ion-margin-end"
        >{value}
        </IonChip>
        <IonButton
          size="small"
          fill="outline"
          onClick={generateNewUuid}
          disabled={disabled || readonly}
          className="ion-margin-top"
        >
          Generate New UUID
        </IonButton>
      </div>
    </IonItem>
  );
}
