import { ChangeEvent, FocusEvent } from 'react';
import { IonInput, IonItem, IonLabel, IonList, IonToolbar } from '@ionic/react';
import IonUuidWidget from '../widgets/IonUuidWidget';
import {
  ariaDescribedByIds,
  BaseInputTemplateProps,
  examplesId,
  getInputProps,
  labelValue,
  FormContextType,
  RJSFSchema,
  StrictRJSFSchema,
} from '@rjsf/utils';

/** The `BaseInputTemplate` is the template to use to render the basic `<IonInput>` component.
 * It is used as the template for rendering many of the input based widgets that differ by `type` and callbacks only.
 *
 * @param props - The `BaseInputTemplateProps` for this template
 */
export default function IonBaseInputTemplate<
  T = any,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = any
>(props: BaseInputTemplateProps<T, S, F>) {
  const {
    id,
    placeholder,
    required,
    readonly,
    disabled,
    type,
    label,
    hideLabel,
    value,
    onChange,
    registry,
    onChangeOverride,
    onBlur,
    onFocus,
    autofocus,
    options,
    schema,
    rawErrors = [],    
  } = props;

  const inputProps = getInputProps<T, S, F>(schema, type, options);
  const { step, min, max, ...rest } = inputProps;

  const _onChange = (value: string | number | null | undefined) =>
    onChange(value === '' || value === null || value === undefined ? options.emptyValue : value);
  const _onBlur = ({ target }: FocusEvent<HTMLIonInputElement>) =>
    onBlur(id, target.value ?? options.emptyValue);
  const _onFocus = ({ target }: FocusEvent<HTMLIonInputElement>) =>
    onFocus(id, target.value ?? options.emptyValue);

  if (type === 'uuid') {
    return (
      <IonUuidWidget
        id={id}
        value={value}
        required={required}
        disabled={disabled || readonly}
        readonly={readonly}
        onChange={onChange}
        onBlur={onBlur}
        onFocus={onFocus}
        label={label}
        hideLabel={hideLabel}
        options={options}
        schema={schema}
        rawErrors={rawErrors} name={''} registry={registry}      />
    );
  }

  return (
    <>
      <IonToolbar color='light'>
        {/* {!hideLabel && <IonLabel position="floating">{label || undefined}</IonLabel>} */}
        <IonInput
          id={id}
          name={id}
          inputMode={type === 'number' ? 'decimal' : 'text'}
          placeholder={placeholder}
          type={type}
          required={required}
          disabled={disabled || readonly}
          readonly={readonly}
          value={value || value === 0 ? value : ''}
          min={min}
          max={max}
          autoFocus={autofocus}
          onIonChange={(e) => _onChange(e.detail.value)}
          onBlur={_onBlur}
          onFocus={_onFocus}
          aria-describedby={ariaDescribedByIds<T>(id, !!schema.examples)}
        />
      </IonToolbar>
      {Array.isArray(schema.examples) && (
        <datalist id={examplesId<T>(id)}>
          {(schema.examples as string[])
            .concat(schema.default && !schema.examples.includes(schema.default) ? ([schema.default] as string[]) : [])
            .map((example: any) => {
              return <option key={example} value={example} />;
            })}
        </datalist>
      )}
    </>
  );
}
