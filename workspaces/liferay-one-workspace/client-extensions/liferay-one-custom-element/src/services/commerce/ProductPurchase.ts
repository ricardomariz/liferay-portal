/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import FetcherError from '~/services/fetcher/FetcherError';
import CommerceUI from '~/services/headless/CommerceUI';
import HeadlessCommerceDeliveryCart from '~/services/headless/HeadlessCommerceDeliveryCart';
import {Analytics} from '~/services/liferay/Analytics';
import {Liferay} from '~/services/liferay/liferay';
import {getSiteURL} from '~/utils/siteUtils';

import type {Account} from '~/types/accounts';
import type {Cart, OrderTypes} from '~/types/orders';
import type {DeliveryProduct} from '~/types/product';

export default class ProductPurchase {
	protected orderTypeExternalReferenceCode?: OrderTypes;
	protected HeadlessCommerceDeliveryCart = HeadlessCommerceDeliveryCart;

	constructor(
		protected readonly account: Account,
		protected readonly product: DeliveryProduct
	) {}

	protected getCart() {
		return {
			accountId: this.account?.id,
			cartItems: this.getCartItems(),
			currencyCode: Liferay.CommerceContext.currency.currencyCode,
			orderTypeExternalReferenceCode: this.orderTypeExternalReferenceCode,
		} as Cart;
	}

	public async getNextStepsLink(cart: Cart) {
		return `/next-steps?orderId=${cart.id}`;
	}

	public async getPaymentNextStepsLink(cart: Cart) {
		const callback = `${window.location.origin}${getSiteURL()}/next-steps?orderId=${cart.id}`;

		const url = await HeadlessCommerceDeliveryCart.getPaymentMethodURL(
			cart.id,
			callback
		);

		return url || callback;
	}

	protected getCartItems(skuId = this.product.skus[0]?.id) {
		return [
			{
				price: {
					currency: Liferay.CommerceContext.currency.currencyCode,
					discount: 0,
				},
				productId: this.product.productId ?? this.product.id,
				quantity: 1,
				settings: {
					maxQuantity: 1,
				},
				skuId,
			},
		];
	}

	public get calculateTax() {
		return false;
	}

	protected analyticsTrack() {
		Analytics.track('ORDER_CREATION', {
			accountId: this.account.id,
			orderTypeExternalReferenceCode: this.orderTypeExternalReferenceCode,
			productName: this.product.name,
		});
	}

	public async createOrder(cart?: Cart, _options?: unknown): Promise<Cart> {
		const body = {
			...this.getCart(),
			...cart,
		};

		if (this.orderTypeExternalReferenceCode) {
			body.orderTypeExternalReferenceCode =
				this.orderTypeExternalReferenceCode;
		}

		if (
			!body.billingAddress &&
			!body.billingAddressId &&
			this.account.defaultBillingAddressId
		) {
			body.billingAddressId = this.account.defaultBillingAddressId;
		}

		const newCart = await (cart?.id
			? HeadlessCommerceDeliveryCart.updateCart(cart.id, body)
			: HeadlessCommerceDeliveryCart.createCart(
					Liferay.CommerceContext.commerceChannelId,
					body
				));

		const [, checkedOutCart] = await Promise.all([
			CommerceUI.selectAccount(this.account.id),
			HeadlessCommerceDeliveryCart.checkoutCart(newCart.id),
		]);

		if (checkedOutCart.errorMessages?.length) {
			throw new FetcherError(checkedOutCart.errorMessages.join(', '));
		}

		this.analyticsTrack();

		return newCart;
	}
}
