import { IonCard, IonCardContent, IonGrid, IonRow, IonCol, IonList } from '@ionic/react';
import {
  getTemplate,
  getUiOptions,
  ArrayFieldTemplateProps,
  ArrayFieldTemplateItemType,
  FormContextType,
  RJSFSchema,
  StrictRJSFSchema,
} from '@rjsf/utils';
import IonAddButton from './IonAddButton';

export default function ArrayFieldTemplate<
  T = any,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = any
>(props: ArrayFieldTemplateProps<T, S, F>) {
  const { canAdd, disabled, idSchema, uiSchema, items, onAddClick, readonly, registry, required, schema, title } = props;
  const uiOptions = getUiOptions<T, S, F>(uiSchema);
  const ArrayFieldDescriptionTemplate = getTemplate<'ArrayFieldDescriptionTemplate', T, S, F>(
    'ArrayFieldDescriptionTemplate',
    registry,
    uiOptions
  );
  const ArrayFieldItemTemplate = getTemplate<'ArrayFieldItemTemplate', T, S, F>(
    'ArrayFieldItemTemplate',
    registry,
    uiOptions
  );
  const ArrayFieldTitleTemplate = getTemplate<'ArrayFieldTitleTemplate', T, S, F>(
    'ArrayFieldTitleTemplate',
    registry,
    uiOptions
  );

  return (
    <IonCard>
      <IonCardContent>
        <ArrayFieldTitleTemplate
          idSchema={idSchema}
          title={uiOptions.title || title}
          schema={schema}
          uiSchema={uiSchema}
          required={required}
          registry={registry}
        />
        <ArrayFieldDescriptionTemplate
          idSchema={idSchema}
          description={uiOptions.description || schema.description}
          schema={schema}
          uiSchema={uiSchema}
          registry={registry}
        />
        <IonList>
          {items &&
            items.map(({ key, ...itemProps }: ArrayFieldTemplateItemType<T, S, F>) => (
              <ArrayFieldItemTemplate key={key} {...itemProps} />
            ))}
        </IonList>
        {canAdd && (
          <IonGrid>
            <IonRow>
              <IonCol size="12" style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <div style={{ marginTop: '1rem' }}>
                  <IonAddButton
                    onClick={onAddClick}
                    disabled={disabled || readonly}
                    registry={registry}
                    uiSchema={uiSchema as any}
                  />
                </div>
              </IonCol>
            </IonRow>
          </IonGrid>
        )}
      </IonCardContent>
    </IonCard>
  );
}
